package com.encens.khipus.model.production;


import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.CompanyListener;
import com.encens.khipus.model.State;
import com.encens.khipus.model.admin.Company;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NamedQueries({
        @NamedQuery(name = "RawMaterialPayRoll.findRawMaterialProducersWithTotalCollectedAmountByMetaProductAndProductiveZoneBetweenDates",
                    query = "select rawMaterialProducer, sum(collectedRawMaterial.amount) " +
                            "from RawMaterialProducer rawMaterialProducer " +
                            "join rawMaterialProducer.collectedRawMaterialList collectedRawMaterial " +
                            "join collectedRawMaterial.rawMaterialCollectionSession rawMaterialCollectionSession " +
                            "where rawMaterialCollectionSession.productiveZone = :productiveZone " +
                            "and rawMaterialCollectionSession.metaProduct = :metaProduct " +
                            "and rawMaterialCollectionSession.date between :startDate and :endDate " +
                            "group by rawMaterialProducer"),

        @NamedQuery(name = "RawMaterialPayRoll.findCollectedAmountByMetaProductBetweenDates",
                    query = "select rawMaterialCollectionSession.date, rawMaterialProducer, collectedRawMaterial.amount " +
                            "from RawMaterialProducer rawMaterialproducer " +
                            "join rawMaterialProducer.collectedRawMaterialList collectedRawMaterial " +
                            "join collectedRawMaterial.rawMaterialCollectionSession rawMaterialCollectionSession " +
                            "join rawMaterialCollectionSession.productiveZone productiveZone " +
                            "where rawMaterialCollectionSession.metaProduct = :metaProduct " +
                            "and productiveZone = :productiveZone " +
                            "and rawMaterialCollectionSession.date between :startDate and :endDate " +
                            "order by rawMaterialCollectionSession.date "),
        @NamedQuery(name = "RawMaterialPayRoll.totalCountProducersByMetaProductBetweenDates",
                    query = "select rawMaterialCollectionSession.date, count(rawMaterialproducer) " +
                            "from RawMaterialProducer rawMaterialproducer " +
                            "join rawMaterialProducer.collectedRawMaterialList collectedRawMaterial " +
                            "join collectedRawMaterial.rawMaterialCollectionSession rawMaterialCollectionSession " +
                            "join rawMaterialCollectionSession.productiveZone productiveZone " +
                            "where rawMaterialCollectionSession.metaProduct = :metaProduct " +
                            "and productiveZone = :productiveZone " +
                            "and rawMaterialCollectionSession.date between :startDate and :endDate " +
                            "group by rawMaterialCollectionSession.date, rawMaterialCollectionSession, productiveZone "),
        @NamedQuery(name = "RawMaterialPayRoll.findTotalCollectedByMetaProductBetweenDates",
                    query = "select collectionForm.date, collectionRecord.receivedAmount, collectionRecord.weightedAmount " +
                            "from CollectionForm collectionForm " +
                            "join collectionForm.collectionRecordList collectionRecord " +
                            "join collectionRecord.productiveZone productiveZone " +
                            "where collectionForm.metaProduct = :metaProduct " +
                            "and productiveZone = :productiveZone " +
                            "and collectionForm.date between :startDate and :endDate"),

        @NamedQuery(name = "RawMaterialPayRoll.findLasEndDateByMetaProductAndProductiveZone",
                    query = "select max(rawMaterialPayRoll.endDate)" +
                            "from RawMaterialPayRoll rawMaterialPayRoll " +
                            "where rawMaterialPayRoll.metaProduct = :metaProduct " +
                            "and rawMaterialPayRoll.productiveZone = :productiveZone "),

        @NamedQuery(name = "RawMaterialPayRoll.totalCollectedGabBetweenDates",
                query = "select collectedRawMaterial.rawMaterialCollectionSession.date, sum(collectedRawMaterial.amount) " +
                        "from CollectedRawMaterial collectedRawMaterial " +
                        "join collectedRawMaterial.rawMaterialCollectionSession " +
                        "where collectedRawMaterial.rawMaterialCollectionSession.date between :startDate and :endDate " +
                        "and collectedRawMaterial.rawMaterialCollectionSession.productiveZone = :productiveZone " +
                        "and collectedRawMaterial.rawMaterialCollectionSession.metaProduct = :metaProduct " +
                        "group by collectedRawMaterial.rawMaterialCollectionSession.date " +
                        "order by collectedRawMaterial.rawMaterialCollectionSession.date asc"
        ),

        @NamedQuery(name = "RawMaterialPayRoll.differenceRawMaterialBetweenDates",
                query = "select collectionRecord.collectionForm.date, collectionRecord.receivedAmount , collectionRecord.weightedAmount " +
                        "from CollectionRecord collectionRecord " +
                        "join collectionRecord.collectionForm " +
                        "where collectionRecord.collectionForm.date between :startDate and :endDate " +
                        "and collectionRecord.productiveZone = :productiveZone " +
                        "and collectionRecord.collectionForm.metaProduct = :metaProduct " +
                        "order by collectionRecord.collectionForm.date asc"
        ),

        @NamedQuery(name = "RawMaterialPayRoll.getRawMaterialCollentionByProductor",
                query = "select CollectedRawMaterial.rawMaterialCollectionSession.date ,CollectedRawMaterial.amount " +
                        "from CollectedRawMaterial collectedRawMaterial " +
                        "join collectedRawMaterial.rawMaterialCollectionSession " +
                        "where collectedRawMaterial.rawMaterialProducer = :rawMaterialProducer " +
                        "and collectedRawMaterial.rawMaterialCollectionSession.metaProduct = :metaProduct " +
                        "and collectedRawMaterial.rawMaterialCollectionSession.date between :startDate and :endDate " +
                        "order by collectedRawMaterial.rawMaterialCollectionSession.date asc"

        ),
        @NamedQuery(name = "RawMaterialPayRoll.getSumaryTotal",
                query = "select " +
                        //" sum(RawMaterialPayRoll.totalMountCollectdByGAB) - sum(RawMaterialPayRoll.totalDiscountByGAB) as differences, " +
                        //" sum(RawMaterialPayRoll.totalMountCollectdByGAB) - sum(RawMaterialPayRoll.totalDiscountByGAB) as differences, " +
                        " sum(RawMaterialPayRoll.totalWeighedByGAB) as balanceWeight, " +
                        " sum(RawMaterialPayRoll.totalCollectedByGAB) as collected " +
                        "from RawMaterialPayRoll rawMaterialPayRoll " +
                        "where rawMaterialPayRoll.startDate = :startDate " +
                        "and rawMaterialPayRoll.endDate <= :endDate " +
                        "and rawMaterialPayRoll.metaProduct = :metaProduct"
        ),
        @NamedQuery(name = "RawMaterialPayRoll.getDiscounts",
                query = "select " +
                        " sum(rawMaterialPayRoll.totalMountCollectdByGAB) as mount, " +
                        " sum(rawMaterialPayRoll.totalCollectedByGAB) as collected, " +
                        " sum(rawMaterialPayRoll.totalAlcoholByGAB) as alcohol, " +
                        " sum(rawMaterialPayRoll.totalConcentratedByGAB) as concentrated, " +
                        " sum(rawMaterialPayRoll.totalYogourdByGAB) as yogurt, " +
                        " sum(rawMaterialPayRoll.totalRecipByGAB) as recip, " +
                        " sum(rawMaterialPayRoll.totalRetentionGAB) as retention, " +
                        " sum(rawMaterialPayRoll.totalVeterinaryByGAB) as veterinary, " +
                        " sum(rawMaterialPayRoll.totalCreditByGAB) as credit, " +
                        " sum(rawMaterialPayRoll.totalLiquidByGAB ) as totalLiquid, " +
                        " sum(rawMaterialPayRoll.totalDiscountByGAB) as totalDiscount, " +
                        " rawMaterialPayRoll.unitPrice as unitPrice " +
                        "from RawMaterialPayRoll rawMaterialPayRoll " +
                        "where rawMaterialPayRoll.startDate = :startDate " +
                        "and rawMaterialPayRoll.endDate <= :endDate " +
                        "and rawMaterialPayRoll.metaProduct = :metaProduct " +
                        " GROUP BY rawMaterialPayRoll.unitPrice"
        ),

        @NamedQuery(name = "RawMaterialPayRoll.getTotalsRawMaterialPayRoll",
                    query = "select " +
                            "rawMaterialPayRoll.totalCollectedByGAB, " +
                            "rawMaterialPayRoll.totalMountCollectdByGAB, " +
                            "rawMaterialPayRoll.totalRetentionGAB, " +
                            "rawMaterialPayRoll.totalCreditByGAB, " +
                            "rawMaterialPayRoll.totalVeterinaryByGAB, " +
                            "rawMaterialPayRoll.totalAlcoholByGAB, " +
                            "rawMaterialPayRoll.totalConcentratedByGAB, " +
                            "rawMaterialPayRoll.totalYogourdByGAB, " +
                            "rawMaterialPayRoll.totalRecipByGAB, " +
                            "rawMaterialPayRoll.totalDiscountByGAB," +
                            "rawMaterialPayRoll.totalAdjustmentByGAB," +
                            "rawMaterialPayRoll.totalOtherIncomeByGAB," +
                            "rawMaterialPayRoll.totalLiquidByGAB "+
                            "from RawMaterialPayRoll rawMaterialPayRoll " +
                            "where rawMaterialPayRoll.startDate = :startDate " +
                            "and rawMaterialPayRoll.endDate = :endDate " +
                            "and rawMaterialPayRoll.productiveZone = :productiveZone " +
                            "and rawMaterialPayRoll.metaProduct = :metaProduct "
        )
})

@TableGenerator(name = "RawMaterialPayRoll_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "PLANILLAPAGOMATERIAPRIMA",
        allocationSize = 10)

@Entity
@Table(name = "PLANILLAPAGOMATERIAPRIMA")
@Filter(name = "companyFilter")
@EntityListeners(CompanyListener.class)
public class RawMaterialPayRoll implements BaseModel {

    @Id
    @Column(name = "IDPLANILLAPAGOMATERIAPRIMA", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "RawMaterialPayRoll_Generator")
    private Long id;

    @Column(name = "FECHAINICIO", columnDefinition = "DATE" ,nullable = false)
    private Date startDate;

    @Column(name = "FECHAFIN", columnDefinition = "DATE" , nullable = false)
    private Date endDate;

    @Column(name = "PRECIOUNITARIO",columnDefinition = "NUMBER(9,2)", nullable = false)
    private double unitPrice;

    @Column(name = "TASAIMPUESTO",columnDefinition = "NUMBER(3,2)",nullable = false)
    private double taxRate;

    @Column(name = "ESTADO", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private State state = State.DRAFT;

    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", nullable = false, updatable = false, insertable = true)
    private Company company;

    @OneToMany(mappedBy = "rawMaterialPayRoll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<RawMaterialPayRecord> rawMaterialPayRecordList = new ArrayList<RawMaterialPayRecord>();

    @OneToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "IDZONAPRODUCTIVA", nullable = false, updatable = false, insertable = true)
    private ProductiveZone productiveZone;

    @OneToOne
    @JoinColumn(name = "IDMETAPRODUCTOPRODUCCION", nullable = false, updatable = false, insertable = true)
    private MetaProduct metaProduct;

    @Column(name = "TOTALACOPIADOXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalWeighedByGAB = 0.0;

    @Column(name = "TOTALPESADOXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalCollectedByGAB = 0.0;

    @Column(name = "TOTALMONTOACOPIOADOXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalMountCollectdByGAB = 0.0;

    @Column(name = "TOTALRETENCIONESXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalRetentionGAB = 0.0;

    @Column(name = "TOTALCREDITOXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalCreditByGAB = 0.0;

    @Column(name = "TOTALVETERINARIOXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalVeterinaryByGAB = 0.0;

    @Column(name = "TOTALALCOHOLXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalAlcoholByGAB = 0.0;

    @Column(name = "TOTALCONCENTRADOSXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalConcentratedByGAB = 0.0;

    @Column(name = "TOTALYOGURDXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalYogourdByGAB = 0.0;

    @Column(name = "TOTALTACHOSXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalRecipByGAB = 0.0;

    @Column(name = "TOTADESCUENTOSXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalDiscountByGAB = 0.0;

    @Column(name = "TOTALOTROSDECUENTOSXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalOtherDiscountByGAB = 0.0;

    @Column(name = "TOTALAJUSTEXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalAdjustmentByGAB = 0.0;

    @Column(name = "TOTALOTROSINGRESOSXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalOtherIncomeByGAB = 0.0;

    @Column(name = "TOTALIQUIDOXGAB", columnDefinition = "NUMBER(16,2)", nullable = false)
    private double totalLiquidByGAB = 0.0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public List<RawMaterialPayRecord> getRawMaterialPayRecordList() {
        return rawMaterialPayRecordList;
    }

    public void setRawMaterialPayRecordList(List<RawMaterialPayRecord> rawMaterialPayRecordList) {
        this.rawMaterialPayRecordList = rawMaterialPayRecordList;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public ProductiveZone getProductiveZone() {
        return productiveZone;
    }

    public void setProductiveZone(ProductiveZone productiveZone) {
        this.productiveZone = productiveZone;
    }

    //@Transient
    public String getFullNameOfProductiveZone() {
        return (productiveZone == null ? "" : productiveZone.getFullName());
    }

    public void setFullNameOfProductiveZone(String fullName) {

    }

    public MetaProduct getMetaProduct() {
        return metaProduct;
    }

    public void setMetaProduct(MetaProduct metaProduct) {
        this.metaProduct = metaProduct;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    public double getTotalCollectedByGAB() {
        return totalCollectedByGAB;
    }

    public void setTotalCollectedByGAB(double totalCollectedByGAB) {
        this.totalCollectedByGAB = totalCollectedByGAB;
    }

    public double getTotalMountCollectdByGAB() {
        return totalMountCollectdByGAB;
    }

    public void setTotalMountCollectdByGAB(double totalMountCollectdByGAB) {
        this.totalMountCollectdByGAB = totalMountCollectdByGAB;
    }

    public double getTotalRetentionGAB() {
        return totalRetentionGAB;
    }

    public void setTotalRetentionGAB(double totalRetentionGAB) {
        this.totalRetentionGAB = totalRetentionGAB;
    }

    public double getTotalCreditByGAB() {
        return totalCreditByGAB;
    }

    public void setTotalCreditByGAB(double totalCreditByGAB) {
        this.totalCreditByGAB = totalCreditByGAB;
    }

    public double getTotalVeterinaryByGAB() {
        return totalVeterinaryByGAB;
    }

    public void setTotalVeterinaryByGAB(double totalVeterinaryByGAB) {
        this.totalVeterinaryByGAB = totalVeterinaryByGAB;
    }

    public double getTotalYogourdByGAB() {
        return totalYogourdByGAB;
    }

    public void setTotalYogourdByGAB(double totalYogourdByGAB) {
        this.totalYogourdByGAB = totalYogourdByGAB;
    }

    public double getTotalRecipByGAB() {
        return totalRecipByGAB;
    }

    public void setTotalRecipByGAB(double totalRecipByGAB) {
        this.totalRecipByGAB = totalRecipByGAB;
    }

    public double getTotalDiscountByGAB() {
        return totalDiscountByGAB;
    }

    public void setTotalDiscountByGAB(double totalDiscountByGAB) {
        this.totalDiscountByGAB = totalDiscountByGAB;
    }

    public double getTotalAdjustmentByGAB() {
        return totalAdjustmentByGAB;
    }

    public void setTotalAdjustmentByGAB(double totalAdjustmentByGAB) {
        this.totalAdjustmentByGAB = totalAdjustmentByGAB;
    }

    public double getTotalOtherIncomeByGAB() {
        return totalOtherIncomeByGAB;
    }

    public void setTotalOtherIncomeByGAB(double totalOtherIncomeByGAB) {
        this.totalOtherIncomeByGAB = totalOtherIncomeByGAB;
    }

    public double getTotalLiquidByGAB() {
        return totalLiquidByGAB;
    }

    public void setTotalLiquidByGAB(double totalLiquidByGAB) {
        this.totalLiquidByGAB = totalLiquidByGAB;
    }

    public double getTotalOtherDiscountByGAB() {
        return totalOtherDiscountByGAB;
    }

    public void setTotalOtherDiscountByGAB(double totalOtherDiscountByGAB) {
        this.totalOtherDiscountByGAB = totalOtherDiscountByGAB;
    }

    public double getTotalAlcoholByGAB() {
        return totalAlcoholByGAB;
    }

    public void setTotalAlcoholByGAB(double totalAlcoholByGAB) {
        this.totalAlcoholByGAB = totalAlcoholByGAB;
    }

    public double getTotalConcentratedByGAB() {
        return totalConcentratedByGAB;
    }

    public void setTotalConcentratedByGAB(double totalConcentratedByGAB) {
        this.totalConcentratedByGAB = totalConcentratedByGAB;
    }
}
