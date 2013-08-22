package main.com.encens.khipus.model.production;

import org.hibernate.annotations.Filter;
import org.hibernate.validator.NotNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 22-05-13
 * Time: 05:13 PM
 * To change this template use File | Settings | File Templates.
 */

@NamedQueries({
    @NamedQuery(name = "ProductiveZone.findAll", query = "select productiveZone from ProductiveZone productiveZone")
    /*@NamedQuery(name = "ProductiveZone.findAllThatDoNotHaveCollectionFormOnDate",
                query = "select productiveZone " +
                        "from ProductiveZone  productiveZone " +
                        "where NOT EXISTS ( " +
                        "   select collectionForm " +
                        "   from CollectionForm collectionForm " +
                        "   where collectionForm.productiveZone.id = productiveZone.id and collectionForm.date = :date" +
                        ")")*/
})

@TableGenerator(name = "ProductiveZone_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "PRODUCTIVEZONE",
        allocationSize = 10)

@Entity
@Table(name = "ZONAPRODUCTIVA", uniqueConstraints = @UniqueConstraint(columnNames = {"NUMERO", "GRUPO", "NOMBRE", "IDCOMPANIA"}))
@Filter(name = "companyFilter")
@EntityListeners(com.encens.khipus.model.CompanyListener.class)
public class ProductiveZone implements com.encens.khipus.model.BaseModel {

    @Id
    @Column(name = "IDZONAPRODUCTIVA", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "ProductiveZone_Generator")
    private Long id;

    @Column(name = "NUMERO", nullable = false, length = 20)
    private String number;

    @Column(name = "GRUPO", nullable = false, length = 20)
    private String group;

    @Column(name = "NOMBRE", nullable = false, length = 200)
    private String name;

    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", nullable = false, updatable = false, insertable = true)
    @NotNull
    private com.encens.khipus.model.admin.Company company;

    @OneToMany(mappedBy = "productiveZone", fetch = FetchType.LAZY)
    private List<CollectionRecord> collectionRecordList = new ArrayList<CollectionRecord>();

    public com.encens.khipus.model.admin.Company getCompany() {
        return company;
    }

    public void setCompany(com.encens.khipus.model.admin.Company company) {
        this.company = company;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getFullName() {
        return "GAB-" + getNumber() + " " + getName() + "(" + getGroup() + ")";
    }

    public void setFullName(String fullName) {
    }

    public List<CollectionRecord> getCollectionRecordList() {
        return collectionRecordList;
    }

    public void setCollectionRecordList(List<CollectionRecord> collectionRecordList) {
        this.collectionRecordList = collectionRecordList;
    }
}
