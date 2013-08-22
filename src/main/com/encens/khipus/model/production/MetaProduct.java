package main.com.encens.khipus.model.production;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/5/13
 * Time: 10:12 AM
 * To change this template use File | Settings | File Templates.
 */
@TableGenerator(name = "MetaProduct_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "METAPRODUCTOPRODUCCION",
        allocationSize = 10)

@Table(name = "METAPRODUCTOPRODUCCION", uniqueConstraints = @UniqueConstraint(columnNames = {"CODIGO", "IDCOMPANIA"}))
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "TIPO", discriminatorType = DiscriminatorType.STRING, length = 20)
@javax.persistence.Entity
@Filter(name = "companyFilter")
@EntityListeners(com.encens.khipus.model.CompanyListener.class)
public class MetaProduct implements Serializable, com.encens.khipus.model.BaseModel {

    @Id
    @Column(name = "IDMETAPRODUCTOPRODUCCION", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "MetaProduct_Generator")
    private Long id;

    @Column(name = "CODIGO", nullable = false, length = 50)
    private String code;

    @Column(name = "NOMBRE", nullable = false, length = 200)
    private String name;

    @Column(name = "DESCRIPCION", nullable = true, length = 500)
    private String description;

    @Column(name = "ESACOPIABLE", nullable = false)
    @Type(type = "IntegerBoolean")
    private Boolean collectable = false;

    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDUNIDADMEDIDA", nullable = true, updatable = true, insertable = true)
    private main.com.encens.khipus.model.production.MeasureUnit measureUnit;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", nullable = false, updatable = false, insertable = true)
    private com.encens.khipus.model.admin.Company company;

    public com.encens.khipus.model.admin.Company getCompany() {
        return company;
    }

    public void setCompany(com.encens.khipus.model.admin.Company company) {
        this.company = company;
    }

    public MeasureUnit getMeasureUnit() {
        return measureUnit;
    }

    public void setMeasureUnit(MeasureUnit measureUnit) {
        this.measureUnit = measureUnit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Transient
    public String getFullName() {
        if (getCode() == null || getName() == null) {
            return "";
        } else {
            return "[" + getCode() + "] " + getName();
        }
    }

    public void setFullName(String fullName) {

    }

    @Transient
    public String getFullNameRawMaterial() {
        if (getCode() == null || getName() == null) {
            return "";
        } else {
            return "[" + getCode() + "] " + getName() + printMeasureUnit();
        }
    }

    public void setFullNameRawMaterial(String fullNameRawMaterial) {

    }

    private String printMeasureUnit() {
        if (measureUnit == null)
            return "";
        else
            return " , " + measureUnit.getName();
    }

    public Boolean getCollectable() {
        return collectable;
    }

    public void setCollectable(Boolean collectable) {
        this.collectable = collectable;
    }
}
