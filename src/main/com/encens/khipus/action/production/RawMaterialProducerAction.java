package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.contacts.Extension;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialProducer;
import com.encens.khipus.service.customers.ExtensionService;
import com.encens.khipus.service.production.ProductiveZoneService;
import com.encens.khipus.service.production.RawMaterialProducerService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.international.StatusMessage;

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

    public List<Extension> extensionList;
    private boolean showExtension = false;

    private List<ProductiveZone> productiveZones;

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

        return super.update();
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
}
