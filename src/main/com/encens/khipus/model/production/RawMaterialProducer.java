package main.com.encens.khipus.model.production;

import com.encens.khipus.model.contacts.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NamedQueries ({
    @NamedQuery(name = "RawMaterialProducer.findAllByProductiveZone",
                query = "select rawMaterialProducer " +
                        "from RawMaterialProducer rawMaterialProducer " +
                        "where rawMaterialProducer.productiveZone = :productiveZone"),
    @NamedQuery(name = "RawMaterialProducer.findReponsibleExceptThisByProductiveZone",
                query = "select rawMaterialProducer " +
                        "from RawMaterialProducer rawMaterialProducer " +
                        "where rawMaterialProducer.responsible = 1 and rawMaterialProducer <> :rawMaterialProducer " +
                        "and rawMaterialProducer.productiveZone = :productiveZone")
})

@Entity
@Table(name = "PRODUCTORMATERIAPRIMA")
@DiscriminatorValue("PRODUCTORMATERIAPRIMA")
@PrimaryKeyJoinColumns(value = {
        @PrimaryKeyJoinColumn(name = "IDPRODUCTORMATERIAPRIMA", referencedColumnName = "ID_PERSONA")})
public class RawMaterialProducer extends Person {

    @Column(name = "LICENCIAIMPUESTOS", length = 200, nullable = true)
    private String codeTaxLicence;

    @Column(name = "FECHAEXPIRALICENCIAIMPUESTOS", nullable = true)
    private Date expirationDateTaxLicence;

    @Column(name = "FECHAINICIALICENCIAIMPUESTOS", nullable = true)
    private Date startDateTaxLicence;

    @Column(name = "ESRESPONSABLE", nullable = false)
    @Type(type = "IntegerBoolean")
    private Boolean responsible;

    @ManyToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDZONAPRODUCTIVA", nullable = true, updatable = true, insertable = true)
    private ProductiveZone productiveZone;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", nullable = false, updatable = false, insertable = true)
    private com.encens.khipus.model.admin.Company company;

    @OneToMany(mappedBy = "rawMaterialProducer", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    //@Filter(name = "companyFilter")
    private List<CollectedRawMaterial> collectedRawMaterialList = new ArrayList<CollectedRawMaterial>(0);

    public String getCodeTaxLicence() {
        return codeTaxLicence;
    }

    public void setCodeTaxLicence(String codeTaxLicence) {
        this.codeTaxLicence = codeTaxLicence;
    }

    public Date getExpirationDateTaxLicence() {
        return expirationDateTaxLicence;
    }

    public void setExpirationDateTaxLicence(Date expirationDateTaxLicence) {
        this.expirationDateTaxLicence = expirationDateTaxLicence;
    }

    public Boolean getResponsible() {
        return responsible;
    }

    public void setResponsible(Boolean responsible) {
        this.responsible = responsible;
    }

    public ProductiveZone getProductiveZone() {
        return productiveZone;
    }

    public void setProductiveZone(ProductiveZone productiveZone) {
        this.productiveZone = productiveZone;
    }

    public com.encens.khipus.model.admin.Company getCompany() {
        return company;
    }


    public void setCompany(com.encens.khipus.model.admin.Company company) {
        this.company = company;
    }

    public List<CollectedRawMaterial> getCollectedRawMaterialList() {
        return collectedRawMaterialList;
    }

    public void setCollectedRawMaterialList(List<CollectedRawMaterial> collectedRawMaterialList) {
        this.collectedRawMaterialList = collectedRawMaterialList;
    }

    public String getFullNameOfProductiveZone() {
        return (productiveZone == null ? "" : productiveZone.getFullName());
    }

    public void setFullNameOfProductiveZone(String fullName) {

    }

    public Date getStartDateTaxLicence() {
        return startDateTaxLicence;
    }

    public void setStartDateTaxLicence(Date startDateTaxLicence) {
        this.startDateTaxLicence = startDateTaxLicence;
    }
}


