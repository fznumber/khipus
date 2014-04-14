package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.contacts.Extension;
import com.encens.khipus.model.production.*;
import com.encens.khipus.service.customers.ExtensionService;
import com.encens.khipus.service.production.CollectedRawMaterialService;
import com.encens.khipus.service.production.LogProductiveZoneService;
import com.encens.khipus.service.production.ProductiveZoneService;
import com.encens.khipus.service.production.RawMaterialProducerService;
import com.encens.khipus.util.DateUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.international.StatusMessage;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.jboss.seam.international.StatusMessage.Severity.WARN;

@Name("rawMaterialProducerAction")
@Scope(ScopeType.CONVERSATION)
public class RawMaterialProducerAction extends GenericAction<RawMaterialProducer> {

    @In(value = "rawMaterialProducerService")
    private RawMaterialProducerService rawMaterialProducerService;

    @In
    private ExtensionService extensionService;

    @In
    private ProductiveZoneService productiveZoneService;

    @In
    private LogProductiveZoneService logProductiveZoneService;

    @In
    private CollectedRawMaterialService collectedRawMaterialService;

    public List<Extension> extensionList;
    private boolean showExtension = false;

    private List<ProductiveZone> productiveZones;
    private ProductiveZone  productiveZoneConcurrent;

    @Override
    protected GenericService getService() {
        return rawMaterialProducerService;
    }

    @Factory(value = "rawMaterialProducer", scope = ScopeType.STATELESS)
    public RawMaterialProducer initContinent() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameProperty() {
        return "name";
    }

    public void updateShowExtension() {
        extensionList = extensionService.findExtensionsByDocumentType(getInstance().getDocumentType());
        showExtension = extensionList != null && !extensionList.isEmpty();
        if (!showExtension) {
            getInstance().setExtensionSite(null);
        }
    }

    public boolean isShowExtension() {
        return showExtension;
    }

    public void setShowExtension(boolean showExtension) {
        this.showExtension = showExtension;
    }

    public List<ProductiveZone> getProductiveZones() {
        if (productiveZones == null) {
            productiveZones = productiveZoneService.findAll();
        }
        return productiveZones;
    }

    public void selectProductiveZone(ProductiveZone productiveZone) {
        try {
            productiveZoneConcurrent = getInstance().getProductiveZone();
            productiveZone = getService().findById(ProductiveZone.class, productiveZone.getId());
            getInstance().setProductiveZone(productiveZone);
        } catch (Exception ex) {
            log.error(ex);
            facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR, "Common.globalError.description");
        }
    }

    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String startCreate() {
        return Outcome.SUCCESS;
    }

    @Override
    @End(ifOutcome = Outcome.SUCCESS)
    public String update() {
        if (checkLicenseBoundaries() == false) {
            return Outcome.REDISPLAY;
        }
        if(getInstance().getProductiveZone() != productiveZoneConcurrent)
        {
           if(verifySessionsToRawMaterialProducer())
           {

           }
           LogProductiveZone logProductiveZone = new LogProductiveZone();
           logProductiveZone.setDate(new Date());
           logProductiveZone.setProductiveZone(productiveZoneConcurrent);
           logProductiveZone.setRawMaterialProducer(getInstance());
           logProductiveZoneService.createLog(logProductiveZone);
        }

        return super.update();
    }

    private Boolean verifySessionsToRawMaterialProducer()
    {
        Boolean result = false;
        Calendar startDate = Calendar.getInstance();
        startDate.setTime(new Date());
        Calendar endDate = Calendar.getInstance();
        endDate.setTime(new Date());
        if(startDate.get(Calendar.DAY_OF_MONTH) > 15)
        {
            startDate.set(Calendar.DAY_OF_MONTH,16);
        }else{
            startDate.set(Calendar.DAY_OF_MONTH,1);
        }

        if(endDate.get(Calendar.DAY_OF_MONTH) > 15)
        {
            endDate.setTime(DateUtils.getLastDayOfMonth(new Date()));
        }else{
            endDate.set(Calendar.DAY_OF_MONTH,15);
        }

        List<CollectedRawMaterial> collectedRawMaterials = null;//collectedRawMaterialService.getCollectedRawMaterialByProductor(startDate.getTime(), endDate.getTime(), getInstance());
        if(collectedRawMaterials.size() > 0)
        {
            addCompanyConfigurationNotFoundErrorMessage();
        }
        return true;
    }

    protected void addErrorHasSessions() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,"CompanyConfiguration.notFound");
    }

    @Override
    @End(ifOutcome = Outcome.SUCCESS)
    public String create() {
        if (checkLicenseBoundaries() == false) {
            return Outcome.REDISPLAY;
        }

        return super.create();
    }

    private boolean checkLicenseBoundaries() {
        if (getInstance().getStartDateTaxLicence() == null || getInstance().getExpirationDateTaxLicence() == null) {
            return true;
        }

        boolean valid = getInstance().getStartDateTaxLicence().compareTo(getInstance().getExpirationDateTaxLicence()) <= 0;
        if (!valid) {
            facesMessages.addFromResourceBundle(WARN, "RawMaterialProducer.warning.licenseStartDateGreaterThanEndDate");
            return false;
        }

        return true;
    }

    public ProductiveZone getProductiveZoneConcurrent() {
        return productiveZoneConcurrent;
    }

    public void setProductiveZoneConcurrent(ProductiveZone productiveZoneConcurrent) {
        this.productiveZoneConcurrent = productiveZoneConcurrent;
    }
}
