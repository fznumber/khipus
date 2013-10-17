package com.encens.khipus.action.production;

import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.ReferentialIntegrityException;
import com.encens.khipus.exception.production.RawMaterialPayRollException;
import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.employees.Gestion;
import com.encens.khipus.model.employees.GestionPayroll;
import com.encens.khipus.model.employees.Month;
import com.encens.khipus.model.production.Periodo;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialPayRoll;
import com.encens.khipus.service.production.ProductiveZoneService;
import com.encens.khipus.service.production.RawMaterialPayRollService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.encens.khipus.exception.production.RawMaterialPayRollException.*;
import static java.util.Calendar.DAY_OF_MONTH;
import static org.jboss.seam.international.StatusMessage.Severity.ERROR;
import static org.jboss.seam.international.StatusMessage.Severity.WARN;

@Name("rawMaterialPayRollAction")
@Scope(ScopeType.CONVERSATION)
public class RawMaterialPayRollAction extends GenericAction<RawMaterialPayRoll> {

    //private Date month;
    private int fortnight;
    private boolean readonly;
    private boolean generateAll = true;
    private boolean delete = false;
    private Gestion gestion;
    private Month month;
    private Periodo periodo;
    private List<GestionPayroll> gestionPayrollList;

    private List<RawMaterialPayRoll> rawMaterialPayRollList;

    @In
    private RawMaterialPayRollService rawMaterialPayRollService;

    @In
    private ProductiveZoneService productiveZoneService;

    @Override
    protected GenericService getService() {
        return rawMaterialPayRollService;
    }

    @Factory(value = "rawMaterialPayRoll", scope = ScopeType.STATELESS)
    public RawMaterialPayRoll initRawMaterialPayRoll() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameMessage() {
        return "\"" + getInstance().getStartDate() + "\" - \"" + getInstance().getEndDate() + "\"";
    }

    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String startNew() {
        return Outcome.SUCCESS;
    }

    @Factory(value = "periodosPayRollGenerate", scope = ScopeType.STATELESS)
    public Periodo[] getPeriodos() {
        return Periodo.values();
    }

    public int[] getFortnightList() {
        return new int[]{1, 2};
    }

    public int getFortnight() {
        return fortnight;
    }

    public void setFortnight(int fortnight) {
        this.fortnight = fortnight;
    }

    /*public Date getMonth() {
        return month;
    }

    public void setMonth(Date month) {
        this.month = month;
    }*/

    public boolean getReadonly() {
        return readonly;
    }

    @Override
    @End(ifOutcome = Outcome.SUCCESS)
    public String create() {
        if (getInstance().getRawMaterialPayRecordList().size() == 0) {
            facesMessages.addFromResourceBundle(WARN, "RawMaterialPayRoll.warn.thereIsNoRecordsGenerated");
            return Outcome.REDISPLAY;
        }

        try {
            RawMaterialPayRoll rawMaterialPayRoll = getInstance();
            rawMaterialPayRollService.create(rawMaterialPayRoll);
            addCreatedMessage();
            return Outcome.SUCCESS;
        } catch (RawMaterialPayRollException ex) {
            print(ex);
            return Outcome.REDISPLAY;
        } catch (EntryDuplicatedException e) {
            addDuplicatedMessage();
            return Outcome.REDISPLAY;
        } catch (Exception ex) {
            if (ex.getCause() instanceof RawMaterialPayRollException) {
                print((RawMaterialPayRollException)ex.getCause());
                return Outcome.REDISPLAY;
            } else {
                facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
                return Outcome.REDISPLAY;
            }
        }
    }

    private void print(RawMaterialPayRollException ex) {
        if (ex.getCode() == CROSS_WITH_ANOTHER_PAYROLL) {
            facesMessages.addFromResourceBundle(WARN, "RawMaterialPayRoll.warning.StartDateCrossWithAnotherPayRoll", ex.getDate());
        } else
        if (ex.getCode() == MINIMUM_START_DATE) {
            facesMessages.addFromResourceBundle(WARN, "RawMaterialPayRoll.warning.StartDateMustBe", ex.getDate());
        } else
        if (ex.getCode() == NO_COLLECTION_ON_DATE) {
            facesMessages.addFromResourceBundle(WARN, "RawMaterialPayRoll.warning.NoCollectionOnDate", ex.getDate());
        } else {
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }

    @Begin(join = true, ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String selectJoin(RawMaterialPayRoll instance) {
        return select(instance);
    }

    public void selectProductiveZone(ProductiveZone productiveZone) {
        try {
            productiveZone = getService().findById(ProductiveZone.class, productiveZone.getId());
            getInstance().setProductiveZone(productiveZone);
            generateAll = false;
        } catch (Exception ex) {
            log.error("Caught Error", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }

    public String generate() {
        try {
                RawMaterialPayRoll rawMaterialPayRoll = getInstance();
                Calendar dateIni = Calendar.getInstance();
                Calendar dateEnd = Calendar.getInstance();
                dateIni.set(gestion.getYear(),month.getValue(),periodo.getInitDay());
                dateEnd.set(gestion.getYear(),month.getValue(),periodo.getEndDay(month.getValue() + 1, gestion.getYear()));
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

                rawMaterialPayRoll.setStartDate(dateFormat.parse(dateFormat.format(dateIni.getTime())));
                rawMaterialPayRoll.setEndDate(dateFormat.parse(dateFormat.format(dateEnd.getTime())));

                if (rawMaterialPayRoll.getStartDate().compareTo(rawMaterialPayRoll.getEndDate()) > 0) {
                    facesMessages.addFromResourceBundle(WARN, "RawMaterialPayRoll.warning.startDateGreaterThanEndDate");
                    return Outcome.FAIL;
                }
                if(rawMaterialPayRoll.getProductiveZone() != null)
                {
                    rawMaterialPayRollService.validate(rawMaterialPayRoll);
                    rawMaterialPayRoll.getRawMaterialPayRecordList().clear();
                    rawMaterialPayRollService.generatePayroll(rawMaterialPayRoll);
                    readonly = true;
                }else
                {
                    return generateAll();
                }
        } catch (RawMaterialPayRollException ex) {
            print(ex);
        } catch (Exception ex) {
            log.error("Caught Error", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
        return Outcome.REDISPLAY;
    }

    public String generateAll() {
        try {

            RawMaterialPayRoll rawMaterialPayRoll = getInstance();
            Calendar dateIni = Calendar.getInstance();
            Calendar dateEnd = Calendar.getInstance();
            dateIni.set(gestion.getYear(),month.getValue(),periodo.getInitDay());
            dateEnd.set(gestion.getYear(),month.getValue(),periodo.getEndDay(month.getValue()+1,gestion.getYear()));
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

            rawMaterialPayRoll.setStartDate(dateFormat.parse(dateFormat.format(dateIni.getTime())));
            rawMaterialPayRoll.setEndDate(dateFormat.parse(dateFormat.format(dateEnd.getTime())));

            List<ProductiveZone> productiveZones = productiveZoneService.findAllThatDoNotHaveCollectionForm(rawMaterialPayRoll.getStartDate(),rawMaterialPayRoll.getEndDate());
            for(ProductiveZone productiveZone: productiveZones)
            {
                RawMaterialPayRoll payRoll = new RawMaterialPayRoll();
                payRoll.setEndDate(rawMaterialPayRoll.getEndDate());
                payRoll.setStartDate(rawMaterialPayRoll.getStartDate());
                payRoll.setCompany(rawMaterialPayRoll.getCompany());
                payRoll.setMetaProduct(rawMaterialPayRoll.getMetaProduct());
                payRoll.setUnitPrice(rawMaterialPayRoll.getUnitPrice());
                payRoll.setTaxRate(rawMaterialPayRoll.getTaxRate());
                payRoll.setProductiveZone(productiveZone);
                rawMaterialPayRollService.validate(payRoll);
                rawMaterialPayRoll.getRawMaterialPayRecordList().clear();
                rawMaterialPayRollService.generatePayroll(payRoll);
                rawMaterialPayRollService.createAll(payRoll);
            }

        } catch (RawMaterialPayRollException ex) {
            print(ex);
        } catch (EntryDuplicatedException e) {
            addDuplicatedMessage();
            return Outcome.REDISPLAY;
        } catch (Exception ex) {
            if (ex.getCause() instanceof RawMaterialPayRollException) {
                print((RawMaterialPayRollException)ex.getCause());
                return Outcome.REDISPLAY;
            } else {
                facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
                return Outcome.REDISPLAY;
            }
        }
        return Outcome.SUCCESS;
    }

    public String deleteAll() throws ReferentialIntegrityException, ConcurrencyException, ParseException {
        RawMaterialPayRoll rawMaterialPayRoll = getInstance();
        Calendar dateIni = Calendar.getInstance();
        Calendar dateEnd = Calendar.getInstance();
        dateIni.set(gestion.getYear(),month.getValue(),periodo.getInitDay());
        dateEnd.set(gestion.getYear(),month.getValue(),periodo.getEndDay(month.getValue()+1,gestion.getYear()));
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

        rawMaterialPayRoll.setStartDate(dateFormat.parse(dateFormat.format(dateIni.getTime())));
        rawMaterialPayRoll.setEndDate(dateFormat.parse(dateFormat.format(dateEnd.getTime())));

        List<RawMaterialPayRoll> rawMaterialPayRolls = rawMaterialPayRollService.findAll(rawMaterialPayRoll.getStartDate(),rawMaterialPayRoll.getEndDate(),rawMaterialPayRoll.getMetaProduct());
        for(RawMaterialPayRoll payRoll: rawMaterialPayRolls)
        {
            rawMaterialPayRollService.delete(payRoll);
        }
        return Outcome.SUCCESS;
    }

    public void redefine() {
        getInstance().getRawMaterialPayRecordList().clear();
        getInstance().setTotalLiquidByGAB(0.0);
        getInstance().setTotalAdjustmentByGAB(0.0);
        getInstance().setTotalCollectedByGAB(0.0);
        getInstance().setTotalConcentratedByGAB(0.0);
        getInstance().setTotalAlcoholByGAB(0.0);
        getInstance().setTotalYogourdByGAB(0.0);
        getInstance().setTotalCreditByGAB(0.0);
        getInstance().setTotalDiscountByGAB(0.0);
        getInstance().setTotalMountCollectdByGAB(0.0);
        getInstance().setTotalOtherDiscountByGAB(0.0);
        getInstance().setTotalVeterinaryByGAB(0.0);
        getInstance().setTotalRetentionGAB(0.0);
        getInstance().setTotalRecipByGAB(0.0);
        readonly = false;
    }

    public List<RawMaterialPayRoll> getRawMaterialPayRollList() {
        return rawMaterialPayRollList;
    }

    public void setRawMaterialPayRollList(List<RawMaterialPayRoll> rawMaterialPayRollList) {
        this.rawMaterialPayRollList = rawMaterialPayRollList;
    }

    public boolean isGenerateAll() {
        return generateAll;
    }

    public void setGenerateAll(boolean generateAll) {
        this.generateAll = generateAll;
    }

    public boolean isDelete() {
        List<RawMaterialPayRoll> rawMaterialPayRolls = rawMaterialPayRollService.findAll();
        return (rawMaterialPayRolls.size()>0)? true : false;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public Gestion getGestion() {
        return gestion;
    }

    public void setGestion(Gestion gestion) {
        this.gestion = gestion;
    }

    public Month getMonth() {
        return month;
    }

    public void setMonth(Month month) {
        this.month = month;
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public void setPeriodo(Periodo periodo) {
        this.periodo = periodo;
    }

    public List<GestionPayroll> getGestionPayrollList() {
        return gestionPayrollList;
    }

    public void setGestionPayrollList(List<GestionPayroll> gestionPayrollList) {
        this.gestionPayrollList = gestionPayrollList;
    }

    public void cleanGestionList() {
        setGestionPayrollList(null);
    }

    public String getCompletPeriod()
    {  if(periodo != null || month != null)
        return  periodo.getPeriodoLiteral() +"del mes de "+ month.getMonthLiteral();
       else
        return "";
    }
}
