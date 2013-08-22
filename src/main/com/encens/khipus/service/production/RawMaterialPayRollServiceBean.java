package com.encens.khipus.service.production;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.EntryNotFoundException;
import com.encens.khipus.exception.production.RawMaterialPayRollException;
import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import com.encens.khipus.model.production.*;
import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.PersistenceException;
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
        Map<Long, Aux> map = createMapOfProducers(rawMaterialPayRoll, totalWeight, countProducers);

        for(Aux aux : map.values()) {
            RawMaterialPayRecord record = new RawMaterialPayRecord();
            record.setTotalAmount(aux.collectedAmount);
            record.setProductiveZoneAdjustment(aux.adjustmentAmount);
            record.setEarnedMoney(aux.earnedMoney);

            if (isValidLicence(aux.producer.getCodeTaxLicence(), aux.producer.getStartDateTaxLicence(), aux.producer.getExpirationDateTaxLicence())) {
                record.setTaxLicense(aux.producer.getCodeTaxLicence());
                record.setExpirationDateTaxLicence(aux.producer.getExpirationDateTaxLicence());
                record.setStartDateTaxLicence(aux.producer.getStartDateTaxLicence());
            }

            RawMaterialProducerDiscount discount = rawMaterialProducerDiscountService.prepareDiscount(aux.producer);
            discount.setWithholdingTax(aux.withholdingTax);
            discount.setRawMaterialPayRecord(record);
            record.setRawMaterialProducerDiscount(discount);

            rawMaterialPayRoll.getRawMaterialPayRecordList().add(record);
            record.setRawMaterialPayRoll(rawMaterialPayRoll);
        }

        calculateLiquidPayable(rawMaterialPayRoll);
        return rawMaterialPayRoll;
    }

    private Map<Long, Aux> createMapOfProducers(RawMaterialPayRoll rawMaterialPayRoll, Map<Date, Double> totalWeight, Map<Date, Long> countProducers) throws RawMaterialPayRollException {
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

            Double delta = find(totalWeight, date);
            Long count = find(countProducers, date);
            Double adjustment = delta / count;
            Double earned = (amount + adjustment) * rawMaterialPayRoll.getUnitPrice();
            Double withholding = (hasLicense(rawMaterialProducer, date) ? 0.0 : earned * taxRate);

            aux.collectedAmount += amount;
            aux.adjustmentAmount += delta/count;
            aux.earnedMoney += earned;
            aux.withholdingTax += withholding;
        }
        return map;
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
        return getEntityManager().createNamedQuery(namedQuery)
                                  .setParameter("metaProduct", rawMaterialPayRoll.getMetaProduct())
                                  .setParameter("startDate", rawMaterialPayRoll.getStartDate())
                                  .setParameter("endDate", rawMaterialPayRoll.getEndDate())
                                  .setParameter("productiveZone", rawMaterialPayRoll.getProductiveZone())
                                  .getResultList();
    }

    class Aux {
        public RawMaterialProducer producer;
        public Double collectedAmount = 0.0;
        public Double adjustmentAmount = 0.0;
        public Double earnedMoney = 0.0;
        public Double withholdingTax = 0.0;
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
            record.setLiquidPayable(liquidPayable);
        }
    }

}
