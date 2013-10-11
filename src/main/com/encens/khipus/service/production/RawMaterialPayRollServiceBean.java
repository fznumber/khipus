package com.encens.khipus.service.production;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.EntryNotFoundException;
import com.encens.khipus.exception.production.RawMaterialPayRollException;
import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import com.encens.khipus.model.production.*;
import com.encens.khipus.util.DateUtils;
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
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import static com.encens.khipus.exception.production.RawMaterialPayRollException.*;
import static java.util.Calendar.DAY_OF_MONTH;

@Name("rawMaterialPayRollService")
@Stateless
@AutoCreate
public class RawMaterialPayRollServiceBean extends ExtendedGenericServiceBean implements RawMaterialPayRollService {

    @In
    private RawMaterialProducerDiscountService rawMaterialProducerDiscountService;

    @In
    private SalaryMovementProducerService salaryMovementProducerService;

    @In
    private SalaryMovementGABService salaryMovementGABService;

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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createAll(RawMaterialPayRoll rawMaterialPayRoll) throws EntryDuplicatedException, RawMaterialPayRollException {
        try {
            //validate(rawMaterialPayRoll);
            //Object args = preCreate(rawMaterialPayRoll);
            //processCreate(rawMaterialPayRoll);
            //postCreate(rawMaterialPayRoll, args);
            //getEntityManager().merge(rawMaterialPayRoll);
            //getEntityManager().flush();

            validate(rawMaterialPayRoll);
            Object args = preCreate(rawMaterialPayRoll);
            processCreate(rawMaterialPayRoll);
            postCreate(rawMaterialPayRoll, args);
            getEntityManager().flush();

        } catch (PersistenceException e) { //TODO when hibernate will fix this http://opensource.atlassian.com/projects/hibernate/browse/EJB-382, we have to restore EntityExistsException here.
            log.debug("Persistence error..", e);
            log.info("PersistenceException caught");
            //log.error(e);
            //throw new EntryDuplicatedException(e);
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

    //region borrar

    /*@Override
    public RawMaterialPayRoll generatePayroll(RawMaterialPayRoll rawMaterialPayRoll) throws EntryNotFoundException, RawMaterialPayRollException {
        Map<Date, Double> totalWeight = createMapOfCollectedAmount(rawMaterialPayRoll);
        Map<Date, Long> countProducers = createMapOfTotalProducers(rawMaterialPayRoll);
        Map<Date, Double> totalWeightsByGab = createMapOfCollectedWeights(rawMaterialPayRoll);
        Map<Date, Double> differences = createMapOfDifferencesWeights(rawMaterialPayRoll);
        Map<Long, Aux> map = createMapOfProducers(rawMaterialPayRoll, totalWeight, countProducers,totalWeightsByGab,differences);

        Double totalWeighed = 0.0;
        Double totalAmountCollected = 0.0;
        Double totalPayCollected = 0.0;
        Double totalRetention = 0.0;
        Double totalAlcohol = 0.0;
        Double totalConcentrated = 0.0;
        Double totalCredit = 0.0;
        Double totalVeterinary = 0.0;
        Double totalYogurt = 0.0;
        Double totalCans = 0.0;
        Double totalDiscount = 0.0;
        Double totalIncome = 0.0;
        Double totalAdjustment = 0.0;
        Double totalOtherIncome = 0.0;
        Double totalOtherDiscount = 0.0;
        Double auxcollectedAmount = 0.0;
        Double auxadjustmentAmount = 0.0;
        Double auxearnedMoney = 0.0;
        Double auxwithholdingTax = 0.0;

        for(Aux aux : map.values()) {
            RawMaterialPayRecord record = new RawMaterialPayRecord();
            auxcollectedAmount = RoundUtil.getRoundValue(aux.collectedAmount,2,RoundUtil.RoundMode.SYMMETRIC);
            record.setTotalAmount(auxcollectedAmount);
            auxadjustmentAmount = RoundUtil.getRoundValue(aux.adjustmentAmount,2,RoundUtil.RoundMode.SYMMETRIC);
            record.setProductiveZoneAdjustment(auxadjustmentAmount);
            auxearnedMoney = RoundUtil.getRoundValue(aux.earnedMoney,2,RoundUtil.RoundMode.SYMMETRIC);
            record.setEarnedMoney(auxearnedMoney);
            auxearnedMoney = RoundUtil.getRoundValue(aux.earnedMoney,2,RoundUtil.RoundMode.SYMMETRIC);
            record.setTotalPayCollected(rawMaterialPayRoll.getUnitPrice() * auxcollectedAmount);
            if (isValidLicence(aux.producer.getCodeTaxLicence(), aux.producer.getStartDateTaxLicence(), aux.producer.getExpirationDateTaxLicence())) {
                record.setTaxLicense(aux.producer.getCodeTaxLicence());
                record.setExpirationDateTaxLicence(aux.producer.getExpirationDateTaxLicence());
                record.setStartDateTaxLicence(aux.producer.getStartDateTaxLicence());
            }

            RawMaterialProducerDiscount discount = rawMaterialProducerDiscountService.prepareDiscount(aux.producer);
            auxwithholdingTax = RoundUtil.getRoundValue(aux.withholdingTax,2,RoundUtil.RoundMode.SYMMETRIC);
            discount.setWithholdingTax(auxwithholdingTax);
            discount.setRawMaterialPayRecord(record);
            record.setRawMaterialProducerDiscount(discount);

            rawMaterialPayRoll.getRawMaterialPayRecordList().add(record);
            record.setRawMaterialPayRoll(rawMaterialPayRoll);
            totalAmountCollected += auxcollectedAmount;
            totalAdjustment += auxadjustmentAmount;
            totalPayCollected += auxearnedMoney;
            totalRetention += auxwithholdingTax;


            totalCredit += RoundUtil.getRoundValue(discount.getCredit(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalAlcohol += RoundUtil.getRoundValue(discount.getAlcohol(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalConcentrated += RoundUtil.getRoundValue(discount.getConcentrated(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalVeterinary += RoundUtil.getRoundValue(discount.getVeterinary(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalYogurt += RoundUtil.getRoundValue(discount.getYogurt(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalCans += RoundUtil.getRoundValue(discount.getCans(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalOtherDiscount += RoundUtil.getRoundValue(discount.getOtherDiscount(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalIncome += RoundUtil.getRoundValue(discount.getOtherIncoming(),2, RoundUtil.RoundMode.SYMMETRIC);

        }

        calculateLiquidPayable(rawMaterialPayRoll);
        rawMaterialPayRoll.setTotalCollectedByGAB(totalAmountCollected);
        rawMaterialPayRoll.setTotalMountCollectdByGAB(totalPayCollected);
        rawMaterialPayRoll.setTotalRetentionGAB(totalRetention);
        rawMaterialPayRoll.setTotalCreditByGAB(totalCredit);
        rawMaterialPayRoll.setTotalAlcoholByGAB(totalAlcohol);
        rawMaterialPayRoll.setTotalConcentratedByGAB(totalConcentrated);
        rawMaterialPayRoll.setTotalVeterinaryByGAB(totalVeterinary);
        rawMaterialPayRoll.setTotalYogourdByGAB(totalYogurt);
        rawMaterialPayRoll.setTotalRecipByGAB(totalCans);
        rawMaterialPayRoll.setTotalOtherDiscountByGAB(totalOtherDiscount);
        rawMaterialPayRoll.setTotalAdjustmentByGAB(totalAdjustment);
        rawMaterialPayRoll.setTotalOtherIncomeByGAB(totalIncome);
        return rawMaterialPayRoll;
    }*/
    //endregion
    @Override
    public RawMaterialPayRoll generatePayroll(RawMaterialPayRoll rawMaterialPayRoll) throws EntryNotFoundException, RawMaterialPayRollException {
        Map<Date, Double> totalWeight = createMapOfCollectedAmount(rawMaterialPayRoll);
        Map<Date, Long> countProducers = createMapOfTotalProducers(rawMaterialPayRoll);
        Map<Date, Double> totalWeightsByGab = createMapOfCollectedWeights(rawMaterialPayRoll);
        Map<Date, Double> differences = createMapOfDifferencesWeights(rawMaterialPayRoll);
        Map<Long, Aux> map = createMapOfProducers(rawMaterialPayRoll, totalWeight, countProducers,totalWeightsByGab,differences);
        Double alcoholByGAB = salaryMovementGABService.getAlcoholBayGAB(rawMaterialPayRoll.getProductiveZone(), rawMaterialPayRoll.getStartDate(), rawMaterialPayRoll.getEndDate());

        Double totalWeighed = 0.0;
        Double totalAmountCollected = 0.0;
        Double totalPayCollected = 0.0;
        Double totalRetention = 0.0;
        Double totalAlcohol = 0.0;
        Double totalConcentrated = 0.0;
        Double totalCredit = 0.0;
        Double totalVeterinary = 0.0;
        Double totalYogurt = 0.0;
        Double totalCans = 0.0;
        Double totalDiscount = 0.0;
        Double totalIncome = 0.0;
        Double totalAdjustment = 0.0;
        Double totalOtherIncome = 0.0;
        Double totalOtherDiscount = 0.0;
        Double auxcollectedAmount = 0.0;
        Double auxadjustmentAmount = 0.0;
        Double auxearnedMoney = 0.0;
        Double auxwithholdingTax = 0.0;
        Double auxcollectedTotalMoney = 0.0;
        for(Aux aux : map.values()) {
            RawMaterialPayRecord record = new RawMaterialPayRecord();
            auxcollectedAmount = aux.collectedAmount;
            record.setTotalAmount(RoundUtil.getRoundValue(auxcollectedAmount,2, RoundUtil.RoundMode.SYMMETRIC));
            auxadjustmentAmount = aux.adjustmentAmount;
            record.setProductiveZoneAdjustment(RoundUtil.getRoundValue(auxadjustmentAmount,2, RoundUtil.RoundMode.SYMMETRIC));
            auxearnedMoney = aux.earnedMoney;
            record.setEarnedMoney(RoundUtil.getRoundValue(auxearnedMoney,2, RoundUtil.RoundMode.SYMMETRIC));
            auxearnedMoney = aux.earnedMoney;
            auxcollectedTotalMoney = aux.collectedTotalMoney;
            record.setTotalPayCollected(RoundUtil.getRoundValue(rawMaterialPayRoll.getUnitPrice() * auxcollectedAmount,2, RoundUtil.RoundMode.SYMMETRIC));
            if (isValidLicence(aux.producer.getCodeTaxLicence(), aux.producer.getStartDateTaxLicence(), aux.producer.getExpirationDateTaxLicence())) {
                record.setTaxLicense(aux.producer.getCodeTaxLicence());
                record.setExpirationDateTaxLicence(aux.producer.getExpirationDateTaxLicence());
                record.setStartDateTaxLicence(aux.producer.getStartDateTaxLicence());
            }

            //RawMaterialProducerDiscount discount = rawMaterialProducerDiscountService.prepareDiscount(aux.producer);
            RawMaterialProducerDiscount discount = salaryMovementProducerService.prepareDiscount(aux.producer,rawMaterialPayRoll.getStartDate(),rawMaterialPayRoll.getEndDate());
            discount.setAlcohol(RoundUtil.getRoundValue(alcoholByGAB*(aux.procentaje),2, RoundUtil.RoundMode.SYMMETRIC));
            //discount.setAlcohol(alcoholByGAB*(aux.procentaje));
            auxwithholdingTax = aux.withholdingTax;
            discount.setWithholdingTax(RoundUtil.getRoundValue(auxwithholdingTax,2, RoundUtil.RoundMode.SYMMETRIC));
            discount.setRawMaterialPayRecord(record);
            record.setRawMaterialProducerDiscount(discount);

            rawMaterialPayRoll.getRawMaterialPayRecordList().add(record);
            record.setRawMaterialPayRoll(rawMaterialPayRoll);
            totalAmountCollected += auxcollectedAmount;
            totalAdjustment += auxadjustmentAmount;
            totalPayCollected += auxcollectedTotalMoney;
            totalRetention += auxwithholdingTax;


            totalCredit += discount.getCredit();
            totalAlcohol += discount.getAlcohol();
            totalConcentrated += discount.getConcentrated();
            totalVeterinary += discount.getVeterinary();
            totalYogurt += discount.getYogurt();
            totalCans += discount.getCans();
            totalOtherDiscount += discount.getOtherDiscount();
            totalIncome += discount.getOtherIncoming();

        }


        totalAmountCollected = RoundUtil.getRoundValue(totalAmountCollected,2, RoundUtil.RoundMode.SYMMETRIC);
        totalPayCollected = RoundUtil.getRoundValue(totalPayCollected,2, RoundUtil.RoundMode.SYMMETRIC);
        totalRetention = RoundUtil.getRoundValue(totalRetention,2, RoundUtil.RoundMode.SYMMETRIC);
        totalCredit = RoundUtil.getRoundValue(totalCredit,2, RoundUtil.RoundMode.SYMMETRIC);
        totalAlcohol = RoundUtil.getRoundValue(totalAlcohol,2, RoundUtil.RoundMode.SYMMETRIC);
        totalConcentrated = RoundUtil.getRoundValue(totalConcentrated,2, RoundUtil.RoundMode.SYMMETRIC);
        totalVeterinary = RoundUtil.getRoundValue(totalVeterinary,2, RoundUtil.RoundMode.SYMMETRIC);
        totalYogurt = RoundUtil.getRoundValue(totalYogurt,2, RoundUtil.RoundMode.SYMMETRIC);
        totalCans = RoundUtil.getRoundValue(totalCans,2, RoundUtil.RoundMode.SYMMETRIC);
        totalOtherDiscount = RoundUtil.getRoundValue(totalOtherDiscount,2, RoundUtil.RoundMode.SYMMETRIC);
        totalAdjustment = RoundUtil.getRoundValue(totalAdjustment,2, RoundUtil.RoundMode.SYMMETRIC);
        totalIncome = RoundUtil.getRoundValue(totalIncome,2, RoundUtil.RoundMode.SYMMETRIC);

        calculateLiquidPayable(rawMaterialPayRoll);
        rawMaterialPayRoll.setTotalCollectedByGAB(totalAmountCollected);
        rawMaterialPayRoll.setTotalMountCollectdByGAB(totalPayCollected);
        rawMaterialPayRoll.setTotalRetentionGAB(totalRetention);
        rawMaterialPayRoll.setTotalCreditByGAB(totalCredit);
        rawMaterialPayRoll.setTotalAlcoholByGAB(totalAlcohol);
        rawMaterialPayRoll.setTotalConcentratedByGAB(totalConcentrated);
        rawMaterialPayRoll.setTotalVeterinaryByGAB(totalVeterinary);
        rawMaterialPayRoll.setTotalYogourdByGAB(totalYogurt);
        rawMaterialPayRoll.setTotalRecipByGAB(totalCans);
        rawMaterialPayRoll.setTotalOtherDiscountByGAB(totalOtherDiscount);
        rawMaterialPayRoll.setTotalAdjustmentByGAB(totalAdjustment);
        rawMaterialPayRoll.setTotalOtherIncomeByGAB(totalIncome);
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

    private Map<Date,Double> createMapOfWeights(RawMaterialPayRoll rawMaterialPayRoll)
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

    public Discounts getDiscounts(Calendar dateIni, Calendar dateEnd,ProductiveZone zone, MetaProduct metaProduct)
    {
        Discounts discounts = new Discounts();

        List<Object[]> datas = getEntityManager().createNamedQuery("RawMaterialPayRoll.getDiscounts")
                                .setParameter("startDate", dateIni.getTime(), TemporalType.DATE)
                                .setParameter("endDate", dateEnd.getTime(), TemporalType.DATE)
                                //.setParameter("productiveZone", zone)
                                .setParameter("metaProduct", metaProduct)
                                .getResultList();

        if(datas.size() > 0){
            discounts.mount = ((Double)datas.get(0)[0] !=null) ? ((Double)datas.get(0)[0]).doubleValue() : 0.0 ;
            discounts.collected = ((Double)datas.get(0)[1] !=null) ? ((Double)datas.get(0)[1]).doubleValue() : 0.0 ;
            discounts.alcohol = ((Double)datas.get(0)[2] !=null) ? ((Double)datas.get(0)[2]).doubleValue() : 0.0 ;
            discounts.concentrated = ((Double)datas.get(0)[3] !=null) ? ((Double)datas.get(0)[3]).doubleValue() : 0.0 ;
            discounts.yogurt = ((Double)datas.get(0)[4] !=null) ? ((Double)datas.get(0)[4]).doubleValue() : 0.0 ;
            discounts.recip = ((Double)datas.get(0)[5] !=null) ? ((Double)datas.get(0)[5]).doubleValue() : 0.0 ;
            discounts.retention = ((Double)datas.get(0)[6] !=null) ? ((Double)datas.get(0)[6]).doubleValue() : 0.0 ;
            discounts.veterinary = ((Double)datas.get(0)[7] !=null) ? ((Double)datas.get(0)[7]).doubleValue() : 0.0 ;
            discounts.credit = ((Double)datas.get(0)[8] !=null) ? ((Double)datas.get(0)[8]).doubleValue() : 0.0 ;
            discounts.discount = ((Double)datas.get(0)[9] !=null) ? ((Double)datas.get(0)[9]).doubleValue() : 0.0 ;
            discounts.liquid = ((Double)datas.get(0)[10] !=null) ? ((Double)datas.get(0)[10]).doubleValue() : 0.0 ;
            discounts.otherDiscount = ((Double)datas.get(0)[11] !=null) ? ((Double)datas.get(0)[11]).doubleValue() : 0.0 ;
            discounts.otherIncome = ((Double)datas.get(0)[12] !=null) ? ((Double)datas.get(0)[12]).doubleValue() : 0.0 ;
            discounts.adjustment = ((Double)datas.get(0)[13] !=null) ? ((Double)datas.get(0)[13]).doubleValue() : 0.0 ;
            discounts.unitPrice = ((Double)datas.get(0)[14] !=null) ? ((Double)datas.get(0)[14]).doubleValue() : 0.0 ;
        }else{
            discounts.mount = 0.0 ;
            discounts.collected = 0.0 ;
            discounts.alcohol = 0.0 ;
            discounts.concentrated = 0.0 ;
            discounts.yogurt = 0.0 ;
            discounts.recip = 0.0 ;
            discounts.retention = 0.0 ;
            discounts.veterinary = 0.0 ;
            discounts.credit = 0.0 ;
            discounts.discount = 0.0 ;
            discounts.liquid = 0.0 ;
            discounts.otherDiscount = 0.0;
            discounts.otherIncome = 0.0;
            discounts.adjustment = 0.0;
            discounts.unitPrice = 0.0;
        }

        return discounts;
    }

    public SummaryTotal getSumaryTotal(Calendar dateIni, Calendar dateEnd,ProductiveZone zone, MetaProduct metaProduct)
    {
        SummaryTotal summaryTotal = new SummaryTotal();

        List<Object[]> datas = getEntityManager().createNamedQuery("RawMaterialPayRoll.getSumaryTotal")
                            .setParameter("startDate", dateIni.getTime(), TemporalType.DATE)
                            .setParameter("endDate", dateEnd.getTime(), TemporalType.DATE)
                            //.setParameter("productiveZone", zone)
                            .setParameter("metaProduct", metaProduct)
                            .getResultList();
        //summaryTotal.differencesTotal = ((Double)datas.get(0)[0] !=null) ? (Double)datas.get(0)[0] : 0.0 ;
        summaryTotal.balanceWeightTotal = ((Double)datas.get(0)[0] !=null) ? (Double)datas.get(0)[0] : 0.0 ;
        summaryTotal.collectedTotal = ((Double)datas.get(0)[1] !=null) ? (Double)datas.get(0)[1] : 0.0 ;
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
            /*Double diffs =  RoundUtil.getRoundValue((receivedAmount.doubleValue() * rawMaterialPayRoll.getUnitPrice()),2, RoundUtil.RoundMode.SYMMETRIC) -
                            RoundUtil.getRoundValue((weightedAmount.doubleValue() * rawMaterialPayRoll.getUnitPrice()),2, RoundUtil.RoundMode.SYMMETRIC);*/
            Double diffs =  weightedAmount.doubleValue() * rawMaterialPayRoll.getUnitPrice() - receivedAmount.doubleValue() * rawMaterialPayRoll.getUnitPrice();

            //Double diffs = (receivedAmount.doubleValue() * rawMaterialPayRoll.getUnitPrice()) - (weightedAmount.doubleValue() * rawMaterialPayRoll.getUnitPrice());
            differences.put(date,diffs);
        }
        return differences;
    }

    public Double getBalanceWeightTotal(Double unitPrice, Calendar startDate, Calendar endDate, MetaProduct metaProduct) {

        List<Object[]> datas = findWeights("RawMaterialPayRoll.getWeightedAndCollectedBetweenDates",startDate,endDate,metaProduct);
        Double weight = 0.0;

        for(Object[] obj : datas){
            weight += (Double)obj[1];
        }
        return weight;
    }

    public Double getTotalWeightMoney(double unitPrice,Calendar startDate,Calendar endDate, MetaProduct metaProduct)
    {
        List<Object[]> datas = findWeights("RawMaterialPayRoll.getWeightedAndCollectedBetweenDates",startDate,endDate,metaProduct);
        Double totalWeightMoney = 0.0;
        Double weight = 0.0;

        for(Object[] obj : datas){
            weight += (Double)obj[1];
        }
        totalWeightMoney = weight * unitPrice;
        return totalWeightMoney;
    }

    public Double getTotalMoneyDiff(double unitPrice,Calendar startDate,Calendar endDate, MetaProduct metaProduct)
    {
        List<Object[]> datas = findWeights("RawMaterialPayRoll.getWeightedAndCollectedBetweenDates",startDate,endDate,metaProduct);

        Double totalMoneyDiff = 0.0;
        Double weight = 0.0;
        Double collected = 0.0;

        for(Object[] obj : datas){

            collected += (Double)obj[0];
            weight += (Double)obj[1];
        }

        totalMoneyDiff = (weight - collected) * unitPrice;

        return totalMoneyDiff;
    }

    public Double getTotalDiff(double unitPrice,Calendar startDate,Calendar endDate, MetaProduct metaProduct)
    {
        List<Object[]> datas = findWeights("RawMaterialPayRoll.getWeightedAndCollectedBetweenDates",startDate,endDate,metaProduct);

        Double totalDiff = 0.0;
        Double weight = 0.0;
        Double collected = 0.0;

        for(Object[] obj : datas){

            collected += (Double)obj[0];
            weight += (Double)obj[1];
        }

        totalDiff = weight - collected;

        return totalDiff;
    }


    public Double getTotalWeightMoney(RawMaterialPayRoll rawMaterialPayRoll)
    {
        List<Object[]> datas = findWeights("getTotalWeightedAndCollectedBetweenDates", rawMaterialPayRoll);
        Double totalWeightMoney = 0.0;
        Double weight = 0.0;

        for(Object[] obj : datas){
            weight += (Double)obj[1];
        }
        totalWeightMoney = weight * rawMaterialPayRoll.getUnitPrice();
        return totalWeightMoney;
    }

    public Double getTotalMoneyDiff(RawMaterialPayRoll rawMaterialPayRoll)
    {
        List<Object[]> datas = findWeights("getTotalWeightedAndCollectedBetweenDates",rawMaterialPayRoll);

        Double totalMoneyDiff = 0.0;
        Double weight = 0.0;
        Double collected = 0.0;

        for(Object[] obj : datas){

            collected += (Double)obj[0];
            weight += (Double)obj[1];
        }

        totalMoneyDiff = (weight - collected) * rawMaterialPayRoll.getUnitPrice();

        return totalMoneyDiff;
    }

    private List<Object[]> findWeights(String namedQuery, Calendar startDate,Calendar endDate, MetaProduct metaProduct)
    {
        List<Object[]> result = null;

        try{
            result = getEntityManager().createNamedQuery(namedQuery)
                    .setParameter("startDate", startDate.getTime(),TemporalType.DATE)
                    .setParameter("endDate", endDate.getTime(),TemporalType.DATE)
                            //.setParameter("productiveZone", rawMaterialPayRoll.getProductiveZone())
                    .setParameter("metaProduct", metaProduct)
                    .getResultList();
        }catch(Exception e)
        {

        }
        return result;
    }

    private List<Object[]> findWeights(String namedQuery, RawMaterialPayRoll rawMaterialPayRoll)
    {
        List<Object[]> result = null;

        try{
            result = getEntityManager().createNamedQuery(namedQuery)
                    .setParameter("startDate", rawMaterialPayRoll.getStartDate())
                    .setParameter("endDate", rawMaterialPayRoll.getEndDate())
                    //.setParameter("productiveZone", rawMaterialPayRoll.getProductiveZone())
                    .setParameter("metaProduct", rawMaterialPayRoll.getMetaProduct())
                    .getResultList();
        }catch(Exception e)
        {

        }
        return result;
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
        Double totalMoneyCollectedByGab = 0.0;
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
            aux.collectedTotalMoney += earned;
            aux.withholdingTax += withholding;

            totalMoneyCollectedByGab += earned;
        }
        addProrationAlcohol(map, rawMaterialPayRoll, totalMoneyCollectedByGab);
        addProrationProcentaje(map, rawMaterialPayRoll,totalMoneyCollectedByGab, getDiffMoneyTotalGab(differences));
        //addProration(map,rawMaterialPayRoll,totalWeightsByGab,differences);
        return map;
    }
    public Double getDiffTotalMoney(Map<Date, Double> differences)
    {
        Iterator collections = differences.entrySet().iterator();
        Double totaldiff = 0.0;
        while(collections.hasNext()){

            Map.Entry thisEntry = (Map.Entry) collections.next();
            Double valor = (Double)thisEntry.getValue();
            totaldiff += valor;
        }

        return RoundUtil.getRoundValue(totaldiff,2, RoundUtil.RoundMode.SYMMETRIC);
    }

    private void addProrationAlcohol(Map<Long, Aux> map, RawMaterialPayRoll rawMaterialPayRoll, Double totalMoneyCollected)
    {
        Iterator collections = map.entrySet().iterator();
        while(collections.hasNext()){

            Map.Entry thisEntry = (Map.Entry) collections.next();
            Aux aux = (Aux)thisEntry.getValue();
            Map<Date,Double> rawMaterialCollected = getRawMaterialCollected(aux.producer, rawMaterialPayRoll);
            //Double porcentage =RoundUtil.getRoundValue( ((aux.earnedMoney *100)/totalMoneyCollected)/100,2, RoundUtil.RoundMode.SYMMETRIC);
            //todo: lanzar un mensaje de advertencia
            Double porcentage;
            if(totalMoneyCollected != 0)
                porcentage = ((aux.earnedMoney *100)/totalMoneyCollected)/100;
            else
                porcentage = 0.0;
            //Double porcentage =RoundUtil.getRoundValue( ((aux.earnedMoney *100)/totalMoneyCollected)/100,5, RoundUtil.RoundMode.SYMMETRIC);
            ((Aux) thisEntry.getValue()).totaDiffMoney = totalMoneyCollected;
            ((Aux) thisEntry.getValue()).procentaje = porcentage;
        }
    }

    private void addProration(Map<Long, Aux> map, RawMaterialPayRoll rawMaterialPayRoll, Map<Date, Double> totalCollectedByGab,Map<Date, Double> differences) throws RawMaterialPayRollException
    {
        Iterator collections = map.entrySet().iterator();
        while(collections.hasNext()){

            Map.Entry thisEntry = (Map.Entry) collections.next();
            Aux aux = (Aux)thisEntry.getValue();
            Map<Date,Double> rawMaterialCollected = getRawMaterialCollected(aux.producer, rawMaterialPayRoll);
            Double proration = calculateDelta(rawMaterialCollected, differences, totalCollectedByGab);
            ((Aux) thisEntry.getValue()).adjustmentAmount = proration;
            //((Aux) thisEntry.getValue()).earnedMoney = ((Aux) thisEntry.getValue()).earnedMoney - proration;
        }
    }

    private void addProrationProcentaje(Map<Long, Aux> map, RawMaterialPayRoll rawMaterialPayRoll, Double totalMoneyCollected,Double totalDiference)
    {
        Iterator collections = map.entrySet().iterator();
        while(collections.hasNext()){

            Map.Entry thisEntry = (Map.Entry) collections.next();
            Aux aux = (Aux)thisEntry.getValue();
            Map<Date,Double> rawMaterialCollected = getRawMaterialCollected(aux.producer, rawMaterialPayRoll);
            //Double porcentage =RoundUtil.getRoundValue( ((aux.earnedMoney *100)/totalMoneyCollected)/100,2, RoundUtil.RoundMode.SYMMETRIC);
            //todo: lanzar un mensaje de advertencia
            Double porcentage;
            if(totalMoneyCollected != 0)
                porcentage = ((aux.earnedMoney *100)/totalMoneyCollected)/100;
            else
                porcentage = 0.0;

            Double proration = totalDiference * porcentage;
            ((Aux) thisEntry.getValue()).adjustmentAmount = proration;
            ((Aux) thisEntry.getValue()).earnedMoney = ((Aux) thisEntry.getValue()).earnedMoney + proration;
        }
    }

    private Double getDiffMoneyTotalGab(Map<Date, Double> differences)
    {
        Double total = 0.0;
        Iterator collections = differences.entrySet().iterator();
        while(collections.hasNext()){
            Map.Entry thisEntry = (Map.Entry) collections.next();
            total += (Double)thisEntry.getValue();
        }
        return total;
    }



    private Double calculateDelta(Map<Date, Double> rawMaterialCollected, Map<Date, Double> differences, Map<Date, Double> totalCollectedByGab) throws RawMaterialPayRollException
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
            Double totalWeight = find(totalCollectedByGab,date);

            //aux =RoundUtil.getRoundValue(mountCollected * (diff/totalWeight),2, RoundUtil.RoundMode.SYMMETRIC);
            if(totalWeight != 0)
            aux =mountCollected * (diff/totalWeight);
            else
            aux = 0.0;
            differ = (diff - aux);
            totalBayGab = (totalWeight - mountCollected);
            differences.put(date,differ);
            totalCollectedByGab.put(date,totalBayGab);
            total += aux;
            //System.out.println(date.toString() +" : "+ mountCollected.toString()+" * "+ "("+ diff.toString()+"/"+ totalWeight.toString()+") = "+ aux.toString());
           // total = total;
        }
        total = RoundUtil.getRoundValue(total,2, RoundUtil.RoundMode.SYMMETRIC);
        //System.out.println("Total: "+total.toString());
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

    //region: borrar
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
    //endregion borrar
    class Aux {
        public RawMaterialProducer producer;
        public Double collectedAmount = 0.0;
        public Double adjustmentAmount = 0.0;
        public Double collectedTotalMoney = 0.0;
        public Double earnedMoney = 0.0;
        public Double withholdingTax = 0.0;
        public Double procentaje = 0.0;
        public Double totaDiffMoney= 0.0;
        public Double totalCollected= 0.0;
        public Double totalWeight = 0.0;
        public Double totalCollectedMoney = 0.0;
        public Double totalWeightMoney = 0.0;
    }

    public class Discounts {
        public Double mount;
        public Double collected;
        public Double unitPrice;
        public Double alcohol;
        public Double concentrated;
        public Double yogurt;
        public Double veterinary;
        public Double credit;
        public Double recip;
        public Double discount;
        public Double liquid;
        public Double retention;
        public Double otherDiscount;
        public Double otherIncome;
        public Double adjustment;
    }

    public class SummaryTotal{
        public Double collectedTotal;
        public Double collectedTotalMoney;
        public Double differencesTotal;
        public Double balanceWeightTotal;
    }

    /*@Override
    public void calculateLiquidPayable(RawMaterialPayRoll rawMaterialPayRoll) {
        Double totalLiquidPay = 0.0;
        for(RawMaterialPayRecord record : rawMaterialPayRoll.getRawMaterialPayRecordList()) {
            RawMaterialProducerDiscount discount = record.getRawMaterialProducerDiscount();
            double totalDiscount = 0.0;
            totalDiscount += RoundUtil.getRoundValue(discount.getAlcohol(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalDiscount += RoundUtil.getRoundValue(discount.getConcentrated(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalDiscount += RoundUtil.getRoundValue(discount.getWithholdingTax(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalDiscount += RoundUtil.getRoundValue(discount.getCans(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalDiscount += RoundUtil.getRoundValue(discount.getCredit(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalDiscount += RoundUtil.getRoundValue(discount.getVeterinary(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalDiscount += RoundUtil.getRoundValue(discount.getYogurt(),2, RoundUtil.RoundMode.SYMMETRIC);
            totalDiscount += RoundUtil.getRoundValue(discount.getOtherDiscount(),2, RoundUtil.RoundMode.SYMMETRIC);
            double liquidPayable = record.getEarnedMoney() - totalDiscount + discount.getOtherIncoming();
            totalLiquidPay += liquidPayable;
            record.setLiquidPayable(RoundUtil.getRoundValue(liquidPayable,2, RoundUtil.RoundMode.SYMMETRIC));
        }
        rawMaterialPayRoll.setTotalLiquidByGAB(RoundUtil.getRoundValue(totalLiquidPay,2, RoundUtil.RoundMode.SYMMETRIC));
    }*/

    @Override
    public void calculateLiquidPayable(RawMaterialPayRoll rawMaterialPayRoll) {
        Double totalLiquidPay = 0.0;
        for(RawMaterialPayRecord record : rawMaterialPayRoll.getRawMaterialPayRecordList()) {
            RawMaterialProducerDiscount discount = record.getRawMaterialProducerDiscount();
            double totalDiscount = 0.0;
            totalDiscount += discount.getAlcohol();
            totalDiscount += discount.getConcentrated();
            totalDiscount += discount.getWithholdingTax();
            totalDiscount += discount.getCans();
            totalDiscount += discount.getCredit();
            totalDiscount += discount.getVeterinary();
            totalDiscount += discount.getYogurt();
            totalDiscount += discount.getOtherDiscount();
            double liquidPayable = record.getEarnedMoney() - totalDiscount + discount.getOtherIncoming();
            totalLiquidPay += liquidPayable;
            record.setLiquidPayable(RoundUtil.getRoundValue(liquidPayable,2, RoundUtil.RoundMode.SYMMETRIC));
        }

        rawMaterialPayRoll.setTotalLiquidByGAB(RoundUtil.getRoundValue(totalLiquidPay,2, RoundUtil.RoundMode.SYMMETRIC));
    }

        public RawMaterialPayRoll getTotalsRawMaterialPayRoll(Calendar dateIni, Calendar dateEnd, ProductiveZone productiveZone, MetaProduct metaProduct)
    {

        String query = createQuery(productiveZone,metaProduct, dateIni,dateEnd);
        RawMaterialPayRoll rawMaterialPayRoll = new RawMaterialPayRoll();
        Query queryObj = getEntityManager().createQuery(query)
                        .setParameter("startDate", dateIni.getTime(), TemporalType.DATE)
                        .setParameter("endDate", dateEnd.getTime(), TemporalType.DATE);
        if(productiveZone != null)
            queryObj.setParameter("productiveZone", productiveZone);
        if(metaProduct != null)
            queryObj.setParameter("metaProduct",metaProduct);

        try {
            /*List<Object[]> datas = getEntityManager().createNamedQuery("RawMaterialPayRoll.getTotalsRawMaterialPayRoll")
                                                      .setParameter("startDate", dateIni)
                                                      .setParameter("endDate", dateEnd)
                                                      .setParameter("productiveZone", productiveZone)
                                                      //.setParameter("metaProduct",metaProduct)
                                                      .getResultList();*/
            List<Object[]> datas= queryObj.getResultList();
                    rawMaterialPayRoll.setTotalCollectedByGAB((Double) (datas.get(0)[0]));
                    rawMaterialPayRoll.setTotalMountCollectdByGAB((Double) (datas.get(0)[1]));
                    rawMaterialPayRoll.setTotalRetentionGAB((Double) (datas.get(0)[2]));
                    rawMaterialPayRoll.setTotalCreditByGAB((Double) (datas.get(0)[3]));
                    rawMaterialPayRoll.setTotalVeterinaryByGAB((Double) (datas.get(0)[4]));
                    rawMaterialPayRoll.setTotalAlcoholByGAB((Double) (datas.get(0)[5]));
                    rawMaterialPayRoll.setTotalConcentratedByGAB((Double) (datas.get(0)[6]));
                    rawMaterialPayRoll.setTotalYogourdByGAB((Double) (datas.get(0)[7]));
                    rawMaterialPayRoll.setTotalRecipByGAB((Double) (datas.get(0)[8]));
                    rawMaterialPayRoll.setTotalDiscountByGAB((Double) (datas.get(0)[9]));
                    rawMaterialPayRoll.setTotalAdjustmentByGAB((Double) (datas.get(0)[10]));
                    rawMaterialPayRoll.setTotalOtherIncomeByGAB((Double) (datas.get(0)[11]));
                    rawMaterialPayRoll.setTotalLiquidByGAB((Double) (datas.get(0)[12]));
                    rawMaterialPayRoll.setProductiveZone((ProductiveZone) (datas.get(0)[13]));

        }catch (Exception e) {
            log.debug("Not found totals RawMaterialPayRoll...." + e);
        }

        return rawMaterialPayRoll;
    }

    private String createQuery(ProductiveZone productiveZone, MetaProduct metaProduct, Calendar dateIni, Calendar dateEnd) {
        String restricZone = (productiveZone == null)? "": " and rawMaterialPayRoll.productiveZone = :productiveZone ";
        String restricMeta = (metaProduct == null)? "": " and rawMaterialPayRoll.metaProduct = :metaProduct ";

        return  "select " +
                "sum(rawMaterialPayRoll.totalCollectedByGAB), " +
                "sum(rawMaterialPayRoll.totalMountCollectdByGAB), " +
                "sum(rawMaterialPayRoll.totalRetentionGAB), " +
                "sum(rawMaterialPayRoll.totalCreditByGAB), " +
                "sum(rawMaterialPayRoll.totalVeterinaryByGAB), " +
                "sum(rawMaterialPayRoll.totalAlcoholByGAB), " +
                "sum(rawMaterialPayRoll.totalConcentratedByGAB), " +
                "sum(rawMaterialPayRoll.totalYogourdByGAB), " +
                "sum(rawMaterialPayRoll.totalRecipByGAB), " +
                "sum(rawMaterialPayRoll.totalDiscountByGAB)," +
                "sum(rawMaterialPayRoll.totalAdjustmentByGAB)," +
                "sum(rawMaterialPayRoll.totalOtherIncomeByGAB)," +
                "sum(rawMaterialPayRoll.totalLiquidByGAB), " +
                "sum(rawMaterialPayRoll.productiveZone) " +
                "from RawMaterialPayRoll rawMaterialPayRoll " +
                "where rawMaterialPayRoll.startDate = :startDate " +
                "and rawMaterialPayRoll.endDate <=  :endDate"
                +restricZone+restricMeta;
    }

    @Override
    public List<RawMaterialPayRoll> findAll(Date startDate, Date endDate, MetaProduct metaProduct) {
        List<RawMaterialPayRoll> rawMaterialPayRolls = getEntityManager().createNamedQuery("RawMaterialPayRoll.getMaterialPayRollInDates")
                                                       .setParameter("startDate",startDate,TemporalType.DATE)
                                                       .setParameter("endDate",endDate,TemporalType.DATE)
                                                       .setParameter("metaProduct",metaProduct)
                                                       .getResultList();
        return rawMaterialPayRolls;
    }

    @Override
    public List<RawMaterialPayRoll> findAll() {
        List<RawMaterialPayRoll> rawMaterialPayRolls = getEntityManager().createNamedQuery("RawMaterialPayRoll.getAllMaterialPayRoll")
                                                              .getResultList();
        return rawMaterialPayRolls;
    }
}
