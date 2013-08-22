package main.com.encens.khipus.action.contacts;

import com.encens.hp90.framework.action.GenericAction;
import com.encens.hp90.model.contacts.Extension;
import com.encens.hp90.model.contacts.Person;
import com.encens.hp90.service.customers.ExtensionService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;

import java.util.List;

/**
 * Person action class
 *
 * @author
 * @version 1.0
 */
@Name("personAction")
@Scope(ScopeType.CONVERSATION)
public class PersonAction extends GenericAction<Person> {

    @In
    private ExtensionService extensionService;

    public List<Extension> extensionList;
    private boolean showExtension = false;

    @Factory(value = "person", scope = ScopeType.STATELESS)
    public Person initPerson() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameProperty() {
        return "fullName";
    }

    @Override
    @Begin(ifOutcome = com.encens.hp90.framework.action.Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String select(Person instance) {
        String outCome = super.select(instance);
        updateShowExtension();
        System.out.println("SELECT outCome ---> " + outCome);
        return outCome;
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
}