package main.com.encens.khipus.action.production;

import com.encens.hp90.exception.EntryDuplicatedException;
import com.encens.hp90.exception.production.RawMaterialPayRollException;
import com.encens.hp90.framework.action.GenericAction;
import com.encens.hp90.framework.action.Outcome;
import com.encens.hp90.framework.service.GenericService;
import com.encens.hp90.model.production.RawMaterialPayRoll;
import com.encens.hp90.model.production.ProductiveZone;
import com.encens.hp90.service.production.RawMaterialPayRollService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.international.StatusMessage;

import java.util.Calendar;
import java.util.Date;

import static com.encens.hp90.exception.production.RawMaterialPayRollException.CROSS_WITH_ANOTHER_PAYROLL;
import static com.encens.hp90.exception.production.RawMaterialPayRollException.MINIMUM_START_DATE;
import static com.encens.hp90.exception.production.RawMaterialPayRollException.NO_COLLECTION_ON_DATE;
import static java.util.Calendar.DAY_OF_MONTH;
import static org.jboss.seam.international.StatusMessage.Severity.ERROR;
import static org.jboss.seam.international.StatusMessage.Severity.WARN;

@Name("rawMaterialPayRollAction")
@Scope(ScopeType.CONVERSATION)
public class RawMaterialPayRollAction extends GenericAction<RawMaterialPayRoll> {

    private Date month;
    private int fortnight;
    private boolean readonly;

    @In
    private RawMaterialPayRollService rawMaterialPayRollService;

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

    public int[] getFortnightList() {
        return new int[]{1, 2};
    }

    public int getFortnight() {
        return fortnight;
    }

    public void setFortnight(int fortnight) {
        this.fortnight = fortnight;
    }

    public Date getMonth() {
        return month;
    }

    public void setMonth(Date month) {
        this.month = month;
    }

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

    @Override
    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String select(RawMaterialPayRoll instance) {
        readonly = true;
        setMonth(instance.getStartDate());

        Calendar c = Calendar.getInstance();
        c.setTime(instance.getStartDate());
        if (c.get(DAY_OF_MONTH) == 1) {
            setFortnight(1);
        } else {
            setFortnight(2);
        }

        String outcome = super.select(instance);
        rawMaterialPayRollService.calculateLiquidPayable(getInstance());
        return outcome;
    }

    @Begin(join = true, ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String selectJoin(RawMaterialPayRoll instance) {
        return select(instance);
    }

    public void selectProductiveZone(ProductiveZone productiveZone) {
        try {
            productiveZone = getService().findById(ProductiveZone.class, productiveZone.getId());
            getInstance().setProductiveZone(productiveZone);
        } catch (Exception ex) {
            log.error("Caught Error", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }

    public void generate() {
        try {
            RawMaterialPayRoll rawMaterialPayRoll = getInstance();
            if (rawMaterialPayRoll.getStartDate().compareTo(rawMaterialPayRoll.getEndDate()) > 0) {
                facesMessages.addFromResourceBundle(WARN, "RawMaterialPayRoll.warning.startDateGreaterThanEndDate");
                return;
            }

            rawMaterialPayRollService.validate(rawMaterialPayRoll);

            rawMaterialPayRoll.getRawMaterialPayRecordList().clear();
            rawMaterialPayRollService.generatePayroll(rawMaterialPayRoll);
            readonly = true;
        } catch (RawMaterialPayRollException ex) {
            print(ex);
        } catch (Exception ex) {
            log.error("Caught Error", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }

    public void redefine() {
        getInstance().getRawMaterialPayRecordList().clear();
        readonly = false;
    }
}
