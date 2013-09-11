package com.encens.khipus.service.production;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.EntryNotFoundException;
import com.encens.khipus.exception.production.RawMaterialPayRollException;
import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import com.encens.khipus.model.production.*;
import com.encens.khipus.util.RoundUtil;
import oracle.jdbc.driver.OracleDriver;
import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.joda.time.DateTime;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.PersistenceException;
import java.math.BigDecimal;
import java.util.*;

import static com.encens.khipus.exception.production.RawMaterialPayRollException.*;
import static java.util.Calendar.DAY_OF_MONTH;

@Name("rawMaterialPayRollService")
@Stateless
@AutoCreate
public class RawMaterialPayRollServiceBean extends ExtendedGenericServiceBean implements RawMaterialPayRollService {

    @In
    private RawMaterialProducerDiscountService rawMaterialProducerDiscountService;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void create(RawMaterialPayRoll rawMaterialPayRoll) throws EntryDuplicatedException, RawMaterialPayRollException {
        try {
            validate(rawMaterialPayRoll);
            Object args = preCreate(rawMaterialPayRoll);
            processCreate(rawMaterialPayRoll);
            postCreate(rawMaterialPayRoll, args);
            getEntityManager().flush();
        } catch (PersistenceException e) { //TODO when hibernate will fix this http://opensource.atlassian.com/projects/hibernate/browse/EJB-382, we have to restore EntityExistsException here.
            log.debug("Persistence error..", e);
            log.info("PersistenceException caught");
            //log.error(e);
            throw new EntryDuplicatedException(e);
        }
    }

    @Override
    public void validate(RawMaterialPayRoll rawMaterialPayRoll) throws RawMaterialPayRollException {
        Date lastEndDate = (Date)getEntityManager().createNamedQuery("RawMaterialPayRoll.findLasEndDateByMetaProductAndProductiveZone")
                                                     .setParameter("productiveZone", rawMaterialPayRoll.getProductiveZone())
                                                     .setParameter("metaProduct", rawMaterialPayRoll.getMetaProduct())
                                                     .getSingleResult();

        if (lastEndDate != null && rawMaterialPayRoll.getStartDate().compareTo(lastEndDate) <= 0) {
            throw new RawMaterialPayRollException(CROSS_WITH_ANOTHER_PAYROLL, lastEndDate);
        }

        if (lastEndDate == null) {
            lastEndDate = new Date(0L);
        }

        Calendar c = Calendar.getInstance();
        c.setTime(lastEndDate);
        c.add(DAY_OF_MONTH, 1);
        lastEndDate = c.getTime();

        Date minimumStartDate = (Date)getEntityManager().createNamedQuery("RawMaterialCollectionSession.findMinimumDateOfCollectionSessionByMetaProductBetweenDates")
                                                        .setParameter("productiveZone", rawMaterialPayRoll.getProductiveZone())
                                                        .setParameter("metaProduct", rawMaterialPayRoll.getMetaProduct())
                                                        .setParameter("startDate", lastEndDate)
                                                        .setParameter("endDate", rawMaterialPayRoll.getStartDate())
                                                        .getSingleResult();

        if (minimumStartDate != null && minimumStartDate.compareTo(rawMaterialPayRoll.getStartDate()) < 0) {
            throw new RawMaterialPayRollException(MINIMUM_START_DATE, minimumStartDate);
        }
    }

    @Override
    public List<RawMaterialPayRecordDetailDummy> generateDetails(RawMaterialPayRecord rawMaterialPayRecord) throws RawMaterialPayRollException {
        List<RawMaterialPayRecordDetailDummy> result = new ArrayList<RawMaterialPayRecordDetailDummy>();

        Map<Date, Double> totalWeight = createMapOfCollectedAmount(rawMaterialPayRecord.getRawMaterialPayRoll());
        Map<Date, Long> countProducers = createMapOfTotalProducers(rawMaterialPayRecord.getRawMaterialPayRoll());

        double taxRate = rawMaterialPayRecord.getRawMaterialPayRoll().getTaxRate() / 100;
        List<Object[]> collectedProducers = find("RawMaterialPayRoll.findCollectedAmountByMetaProductBetweenDates", rawMaterialPayRecord.getRawMaterialPayRoll());

        RawMaterialProducer producer = rawMaterialPayRecord.getRawMaterialProducerDiscount().getRawMaterialProducer();
        Double unitPrice = rawMaterialPayRecord.getRawMaterialPayRoll().getUnitPrice();
        for(Object[] obj : collectedProducers) {
            Date date = (Date)obj[0];
            RawMaterialProducer rawMaterialProducer = (RawMaterialProducer)obj[1];
            Double amount = (Double)obj[2];

            if (rawMaterialProducer.getId().equals(producer.getId()) == false) {
                continue;
            }

            Double delta = find(totalWeight, date);
            Long count = find(countProducers, date);
            Double adjustment = delta / count;
            Double earned = (amount + adjustment) * unitPrice;
            Double withholding = (hasLicense(rawMaterialPayRecord, date) ? 0.0 : earned * taxRate);

            RawMaterialPayRecordDetailDummy dummy = new RawMaterialPayRecordDetailDummy();
            dummy.setDate(date);
            dummy.setCollectedAmount(amount);
            dummy.setProductiveZoneDelta(delta);
            dummy.setProductiveZoneAdjustment(adjustment);
            dummy.setTotalProducers(count);
            dummy.setUnitPrice(unitPrice);
            dummy.setWithholding(withholding);
            dummy.setEarned(earned);
            dummy.setGrandTotal(earned - withholding);

            result.add(dummy);
        }
        return result;
    }

    @Override
    public RawMaterialPayRoll generatePayroll(RawMaterialPayRoll rawMaterialPayRoll) throws EntryNotFoundException, RawMaterialPayRollException {
        Map<Date, Double> totalWeight = createMapOfCollectedAmount(rawMaterialPayRoll);
        Map<Date, Long> countProducers = createMapOfTotalProducers(rawMaterialPayRoll);
        Map<Date, Double> totalWeightsByGab = createMapOfCollectedWeights(rawMaterialPayRoll);
        Map<Date, Double> differences = createMapOfDifferencesWeights(rawMaterialPayRoll);
        Map<Long, Aux> map = createMapOfProducers(rawMaterialPayRoll, totalWeight, countProducers,totalWeightsByGab,differences);

        for(Aux aux : map.values()) {
            RawMaterialPayRecord record = new RawMaterialPayRecord();
            record.setTotalAmount(aux.collectedAmount);
            record.setProductiveZoneAdjustment(aux.adjustmentAmount);
            record.setEarnedMoney(aux.earnedMoney);
            record.setTotalPayCollected(rawMaterialPayRoll.getUnitPrice() * aux.collectedAmount);
            if (isValidLicence(aux.producer.getCodeTaxLicence(), aux.producer.getStartDateTaxLicence(), aux.producer.getExpirationDateTaxLicence())) {
                record.setTaxLicense(aux.producer.getCodeTaxLicence());
                record.setExpirationDateTaxLicence(aux.producer.getExpirationDateTaxLicence());
                record.setStartDateTaxLicence(aux.producer.getStartDateTaxLicence());
            }

            RawMaterialProducerDiscount discount = rawMaterialProducerDiscountService.prepareDiscount(aux.producer);
            discount.setWithholdingTax(RoundUtil.getRoundValue(aux.withholdingTax,2, RoundUtil.RoundMode.SYMMETRIC));
            discount.setRawMaterialPayRecord(record);
            record.setRawMaterialProducerDiscount(discount);

            rawMaterialPayRoll.getRawMaterialPayRecordList().add(record);
            record.setRawMaterialPayRoll(rawMaterialPayRoll);
        }

        calculateLiquidPayable(rawMaterialPayRoll);
        return rawMaterialPayRoll;
    }

    private Map<Date,Double> createMapOfCollectedWeights(RawMaterialPayRoll rawMaterialPayRoll)
    {
        List<Object[]> counts = findTotalCollection("RawMaterialPayRoll.totalCollectedGabBetweenDates", rawMaterialPayRoll);
        Map<Date, Double> countProducers = new HashMap<Date, Double>();

        for(Object[] obj : counts) {
            Date date = (Date)obj[0];
            Double count = (Double)obj[1];

            countProducers.put(date, count);
        }

        return countProducers;
    }

    public Discounts getDiscounts(Date dateIni, Date dateEnd,ProductiveZone zone, MetaProduct metaProduct)
    {
        Discounts discounts = new Discounts();

        DateTime dateTimeIni = new DateTime(dateIni);
        DateTime dateTimeEnd = new DateTime(dateEnd);

        String querySql = "select sum(dpm.yogurt) " +
                       "     ,sum(dpm.tachos) " +
                       "     ,sum(dpm.retencion) " +
                       "     ,sum(dpm.veterinario) " +
                       "     ,sum(dpm.credito)" +
                       "     ,ppm.preciounitario " +
                       " from registropagomateriaprima  rpm " +
                       " inner join planillapagomateriaprima ppm " +
                       " on rpm.idplanillapagomateriaprima = ppm.idplanillapagomateriaprima" +
                       " inner join descuentproductmateriaprima dpm " +
                       " on rpm.iddescuentproductmateriaprima = dpm.iddescuentproductmateriaprima " +
                       " where ppm.fechainicio = to_date('"+dateTimeIni.getDayOfMonth()+"/"+dateTimeIni.getMonthOfYear()+"/"+dateTimeIni.getYear()+"','dd/mm/yyyy') " +
                       " and ppm.fechafin = to_date('"+dateTimeEnd.getDayOfMonth()+"/"+dateTimeEnd.getMonthOfYear()+"/"+dateTimeEnd.getYear()+"','dd/mm/yyyy') " +
                       " group by ppm.preciounitario " ;
        List<Object[]> datas = getEntityManager().createNativeQuery(querySql)

                        .getResultList();
        if(datas.size() > 0){
            discounts.yogurt = ((BigDecimal)datas.get(0)[0] !=null) ? ((BigDecimal)datas.get(0)[0]).doubleValue() : 0.0 ;
            discounts.recip = ((BigDecimal)datas.get(0)[1] !=null) ? ((BigDecimal)datas.get(0)[1]).doubleValue() : 0.0 ;
            discounts.retention = ((BigDecimal)datas.get(0)[2] !=null) ? ((BigDecimal)datas.get(0)[2]).doubleValue() : 0.0 ;
            discounts.veterinary = ((BigDecimal)datas.get(0)[3] !=null) ? ((BigDecimal)datas.get(0)[3]).doubleValue() : 0.0 ;
            discounts.credit = ((BigDecimal)datas.get(0)[4] !=null) ? ((BigDecimal)datas.get(0)[4]).doubleValue() : 0.0 ;
            discounts.unitPrice = ((BigDecimal)datas.get(0)[5] !=null) ? ((BigDecimal)datas.get(0)[5]).doubleValue() : 0.0 ;
        }else{
            discounts.yogurt = 0.0 ;
            discounts.recip = 0.0 ;
            discounts.retention = 0.0 ;
            discounts.veterinary = 0.0 ;
            discounts.credit = 0.0 ;
            discounts.unitPrice = 0.0;
        }

        return discounts;
    }
    //todo:el resumen debe ser por zona productiva y producto acopiable
    public SummaryTotal getSumaryTotal(Date dateIni, Date dateEnd,ProductiveZone zone, MetaProduct metaProduct)
    {
        SummaryTotal summaryTotal = new SummaryTotal();

        List<Object[]> datas = getEntityManager().createNamedQuery("RawMaterialPayRoll.getSumaryTotal")
                            .setParameter("startDate", dateIni)
                            .setParameter("endDate", dateEnd)
                            //.setParameter("productiveZone", zone)
                            //.setParameter("metaProduct", metaProduct)
                            .getResultList();
        summaryTotal.differencesTotal = ((Double)datas.get(0)[0] !=null) ? (Double)datas.get(0)[0] : 0.0 ;
        summaryTotal.balanceWeightTotal = ((Double)datas.get(0)[1] !=null) ? (Double)datas.get(0)[1] : 0.0 ;
        summaryTotal.collectedTotal = ((Double)datas.get(0)[2] !=null) ? (Double)datas.get(0)[2] : 0.0 ;
        return summaryTotal;
    }

    private Map<Date,Double> createMapOfDifferencesWeights(RawMaterialPayRoll rawMaterialPayRoll)
    {
        List<Object[]> datas = findDifferencesWeights("RawMaterialPayRoll.differenceRawMaterialBetweenDates", rawMaterialPayRoll);

        Map<Date,Double> differences = new HashMap<Date, Double>();

        for(Object[] obj : datas){
            Date date = (Date)obj[0];
            Double receivedAmount = (Double)obj[1];
            Double weightedAmount = (Double)obj[2];
            //todo: (revisar) si se redondea entonces se pierden decimales que pueden influir en el resultado
            Double diffs =  RoundUtil.getRoundValue((receivedAmount.doubleValue() * rawMaterialPayRoll.getUnitPrice()),2, RoundUtil.RoundMode.SYMMETRIC) -
                            RoundUtil.getRoundValue((weightedAmount.doubleValue() * rawMaterialPayRoll.getUnitPrice()),2, RoundUtil.RoundMode.SYMMETRIC);
            //Double diffs = (receivedAmount.doubleValue() * rawMaterialPayRoll.getUnitPrice()) - (weightedAmount.doubleValue() * rawMaterialPayRoll.getUnitPrice());
            differences.put(date,diffs);
        }
        return differences;
    }

    private List<Object[]> findDifferencesWeights(String namedQuery, RawMaterialPayRoll rawMaterialPayRoll)
    {
        List<Object[]> result = null;

        try{
            result = getEntityManager().createNamedQuery("RawMaterialPayRoll.differenceRawMaterialBetweenDates")
                    .setParameter("startDate", rawMaterialPayRoll.getStartDate())
                    .setParameter("endDate", rawMaterialPayRoll.getEndDate())
                    .setParameter("productiveZone", rawMaterialPayRoll.getProductiveZone())
                    .setParameter("metaProduct", rawMaterialPayRoll.getMetaProduct())
                    .getResultList();
        }catch(Exception e)
        {

        }
        return result;
    }

    private List<Object[]> findTotalCollection(String namedQuery, RawMaterialPayRoll rawMaterialPayRoll)
    {
        List<Object[]> result = null;
        try{
            result = getEntityManager().createNamedQuery(namedQuery)
                    .setParameter("startDate", rawMaterialPayRoll.getStartDate())
                    .setParameter("endDate", rawMaterialPayRoll.getEndDate())
                    .setParameter("productiveZone", rawMaterialPayRoll.getProductiveZone())
                    .setParameter("metaProduct", rawMaterialPayRoll.getMetaProduct())
                    .getResultList();
        }catch (Exception e)
        {

        }

        return result;
    }

    private Map<Long, Aux> createMapOfProducers(RawMaterialPayRoll rawMaterialPayRoll, Map<Date, Double> totalWeight, Map<Date, Long> countProducers, Map<Date, Double> totalWeightsByGab,Map<Date, Double> differences) throws RawMaterialPayRollException {
        double taxRate = rawMaterialPayRoll.getTaxRate() / 100;
        List<Object[]> collectedProducers = find("RawMaterialPayRoll.findCollectedAmountByMetaProductBetweenDates", rawMaterialPayRoll);
        Map<Long, Aux> map = new HashMap<Long, Aux>();
        for(Object[] obj : collectedProducers) {
            Date date = (Date)obj[0];
            RawMaterialProducer rawMaterialProducer = (RawMaterialProducer)obj[1];
            Double amount = (Double)obj[2];

            Aux aux = map.get(rawMaterialProducer.getId());
            if (aux == null) {
                aux = new Aux();
                aux.producer = rawMaterialProducer;
                map.put(rawMaterialProducer.getId(), aux);
            }

            //Double delta = find(totalWeight, date);

            Long count = find(countProducers, date);
            //Double adjustment = delta / count;
            //Double earned = (amount + adjustment) * rawMaterialPayRoll.getUnitPrice();
            Double earned = amount  * rawMaterialPayRoll.getUnitPrice();
            Double withholding = (hasLicense(rawMaterialProducer, date) ? 0.0 : earned * taxRate);

            aux.collectedAmount += amount;
            //aux.adjustmentAmount += delta/count;
            aux.earnedMoney += earned;
            aux.withholdingTax += withholding;
        }
        addProration(map,rawMaterialPayRoll,totalWeightsByGab,differences);
        return map;
    }

    private void addProration(Map<Long, Aux> map, RawMaterialPayRoll rawMaterialPayRoll, Map<Date, Double> totalWeightsByGab,Map<Date, Double> differences) throws RawMaterialPayRollException
    {
        Iterator collections = map.entrySet().iterator();
        while(collections.hasNext()){

            Map.Entry thisEntry = (Map.Entry) collections.next();
            Aux aux = (Aux)thisEntry.getValue();
            Map<Date,Double> rawMaterialCollected = getRawMaterialCollected(aux.producer, rawMaterialPayRoll);
            Double proration = calculateDelta(rawMaterialCollected, differences, totalWeightsByGab);
            ((Aux) thisEntry.getValue()).adjustmentAmount = proration;
            ((Aux) thisEntry.getValue()).earnedMoney = ((Aux) thisEntry.getValue()).earnedMoney - proration;
        }
    }

    private Double calculateDelta(Map<Date, Double> rawMaterialCollected, Map<Date, Double> differences, Map<Date, Double> totalWeightsByGab) throws RawMaterialPayRollException
    {
        Iterator collections = rawMaterialCollected.entrySet().iterator();
        Double total =0.0d;
        Double aux =0.0d;
        Double differ= 0.0d;
        Double totalBayGab= 0.0d;
        while(collections.hasNext()){
            Map.Entry thisEntry = (Map.Entry) collections.next();
            Double mountCollected = (Double)thisEntry.getValue();
            Date date = (Date)thisEntry.getKey();
            Double diff = find(differences,date);
            Double totalWeight = find(totalWeightsByGab,date);

            aux = RoundUtil.getRoundValue((mountCollected * (diff/totalWeight)),2, RoundUtil.RoundMode.SYMMETRIC);
            differ = RoundUtil.getRoundValue((diff - aux),2, RoundUtil.RoundMode.SYMMETRIC);
            totalBayGab = RoundUtil.getRoundValue((totalWeight - mountCollected),2, RoundUtil.RoundMode.SYMMETRIC);
            differences.put(date,differ);
            totalWeightsByGab.put(date,totalBayGab);
            total += aux;
            total = RoundUtil.getRoundValue(total,2, RoundUtil.RoundMode.SYMMETRIC);
        }

        return total;
    }

    private Map<Date, Double> getRawMaterialCollected(RawMaterialProducer rawMaterialProducer, RawMaterialPayRoll rawMaterialPayRoll)
    {
        List<Object[]> datas = findRawMawterilCollected(rawMaterialProducer, rawMaterialPayRoll);
        Map<Date, Double> result = new HashMap<Date, Double>();
        for(Object[] obj : datas) {
            Date date = (Date)obj[0];
            Double count = (Double)obj[1];

            result.put(date, count);
        }
        return result;
    }

    private List<Object[]> findRawMawterilCollected(RawMaterialProducer rawMaterialProducer, RawMaterialPayRoll rawMaterialPayRoll)
    {
        List<Object[]> result = null;

        try{
            result = getEntityManager().createNamedQuery("RawMaterialPayRoll.getRawMaterialCollentionByProductor")
                    .setParameter("startDate", rawMaterialPayRoll.getStartDate())
                    .setParameter("endDate", rawMaterialPayRoll.getEndDate())
                    .setParameter("rawMaterialProducer", rawMaterialProducer)
                    .setParameter("metaProduct", rawMaterialPayRoll.getMetaProduct())
                    .getResultList();
        }catch(Exception e)
        {

        }
        return result;
    }

    private <T> T find(Map<Date, T> map, Date date) throws RawMaterialPayRollException {
        T result = map.get(date);
        if (result == null) {
            throw new RawMaterialPayRollException(NO_COLLECTION_ON_DATE, date);
        }
        return result;
    }

    private Map<Date, Double> createMapOfCollectedAmount(RawMaterialPayRoll rawMaterialPayRoll) {
        List<Object[]> collectedTotal = find("RawMaterialPayRoll.findTotalCollectedByMetaProductBetweenDates", rawMaterialPayRoll);
        Map<Date, Double> totalWeight = new HashMap<Date, Double>();
        for (Object[] obj : collectedTotal) {
            Date date = (Date)obj[0];
            Double received = (Double)obj[1];
            Double weighted = (Double)obj[2];

            totalWeight.put(date, weighted - received);
        }
        return totalWeight;
    }

    private Map<Date, Long> createMapOfTotalProducers(RawMaterialPayRoll rawMaterialPayRoll) {
        List<Object[]> counts = find("RawMaterialPayRoll.totalCountProducersByMetaProductBetweenDates", rawMaterialPayRoll);
        Map<Date, Long> countProducers = new HashMap<Date, Long>();
        for(Object[] obj : counts) {
            Date date = (Date)obj[0];
            Long count = (Long)obj[1];

            countProducers.put(date, count);
        }
        return countProducers;
    }

    private boolean hasLicense(RawMaterialPayRecord rawMaterialPayRecord, Date date) {
        if (!isValidLicence(rawMaterialPayRecord.getTaxLicense(), rawMaterialPayRecord.getStartDateTaxLicence(), rawMaterialPayRecord.getExpirationDateTaxLicence())) return false;
        if (!isDateInRange(date, rawMaterialPayRecord.getStartDateTaxLicence(), rawMaterialPayRecord.getExpirationDateTaxLicence())) return false;

        return true;
    }

    private boolean hasLicense(RawMaterialProducer rawMaterialProducer, Date date) {
        if (!isValidLicence(rawMaterialProducer.getCodeTaxLicence(), rawMaterialProducer.getStartDateTaxLicence(), rawMaterialProducer.getExpirationDateTaxLicence())) return false;
        if (!isDateInRange(date, rawMaterialProducer.getStartDateTaxLicence(), rawMaterialProducer.getExpirationDateTaxLicence())) return false;

        return true;
    }

    private boolean isDateInRange(Date date, Date start, Date end) {
        if (date.compareTo(start) < 0) return false;
        if (date.compareTo(end) > 0) return false;
        return true;
    }

    private boolean isValidLicence(String license, Date startDate, Date endDate) {
        if (endDate == null) return false;
        if (startDate == null) return false;
        if (startDate.compareTo(endDate) > 0) return false;
        if (StringUtils.isBlank(license)) return false;
        return true;
    }


    private List<Object[]> find(String namedQuery, RawMaterialPayRoll rawMaterialPayRoll) {
        List<Object[]> result = null;
        try{
            result = getEntityManager().createNamedQuery(namedQuery)
                    .setParameter("metaProduct", rawMaterialPayRoll.getMetaProduct())
                    .setParameter("startDate", rawMaterialPayRoll.getStartDate())
                    .setParameter("endDate", rawMaterialPayRoll.getEndDate())
                    .setParameter("productiveZone", rawMaterialPayRoll.getProductiveZone())
                    .getResultList();
        }catch (Exception e)
        {

        }
        return result;
    }
    /*
    public List<GeneratedPayroll> findValidGeneratedPayrollsByGestionAndMount(Gestion gestion, Month month) {
        try {
            userTransaction.begin();
            List<GeneratedPayroll> resultList = em.createNamedQuery("GeneratedPayroll.findGeneratedPayrollsByGestionAndType")
                    .setParameter("gestion", gestion)
                    .setParameter("month", month).setParameter("generatedPayrollType", GeneratedPayrollType.OFFICIAL).getResultList();
            userTransaction.commit();
            return resultList;
        } catch (Exception e) {
            try {
                userTransaction.rollback();
            } catch (SystemException e1) {
                log.debug("Rollback failed", e1);
            }
        }
        return new ArrayList<GeneratedPayroll>();
    }
    */

    class Aux {
        public RawMaterialProducer producer;
        public Double collectedAmount = 0.0;
        public Double adjustmentAmount = 0.0;
        public Double earnedMoney = 0.0;
        public Double withholdingTax = 0.0;
    }

    public class Discounts {
        public Double unitPrice;
        public Double yogurt;
        public Double veterinary;
        public Double credit;
        public Double recip;
        public Double retention;
    }

    public class SummaryTotal{
        public Double collectedTotal;
        public Double collectedTotalMoney;
        public Double differencesTotal;
        public Double balanceWeightTotal;
    }

    @Override
    public void calculateLiquidPayable(RawMaterialPayRoll rawMaterialPayRoll) {
        for(RawMaterialPayRecord record : rawMaterialPayRoll.getRawMaterialPayRecordList()) {
            RawMaterialProducerDiscount discount = record.getRawMaterialProducerDiscount();
            double totalDiscount = 0.0;
            totalDiscount += discount.getWithholdingTax();
            totalDiscount += discount.getCans();
            totalDiscount += discount.getCredit();
            totalDiscount += discount.getVeterinary();
            totalDiscount += discount.getYogurt();
            totalDiscount += discount.getOtherDiscount();

            double liquidPayable = record.getEarnedMoney() - totalDiscount + discount.getOtherIncoming();
            record.setLiquidPayable(RoundUtil.getRoundValue(liquidPayable,2, RoundUtil.RoundMode.SYMMETRIC));
        }
    }

}
