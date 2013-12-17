package com.encens.khipus.model.production;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.CompanyListener;
import com.encens.khipus.model.admin.Company;
import com.encens.khipus.model.finances.CashAccount;
import com.encens.khipus.model.warehouse.Group;
import com.encens.khipus.util.Constants;
import org.hibernate.annotations.Filter;
import org.hibernate.validator.Length;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Entity for Employee Time CArd
 *
 * @author Diego Loza
 * @version 1.2.1
 */
@TableGenerator(name = "IndirectCostsConifg.tableGenerator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "COSTOSINDIRECTOS",
        allocationSize = 10)

@Entity
@Table(name = "COSTOSINDIRECTOS")
@Filter(name = Constants.COMPANY_FILTER_NAME)
@EntityListeners(CompanyListener.class)
public class IndirectCostsConifg implements BaseModel {

    @Id
    @Column(name = "IDCOSTOSINDIRECTOS", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "IndirectCostsConifg.tableGenerator")
    private Long id;

    @Column(name = "NOMBRE", nullable = true)
    private String name;

    @Column(name = "MES", nullable = false, columnDefinition = "NUMBER(2)")
    private Integer month;

    @Column(name = "ANIO", nullable = false, columnDefinition = "NUMBER(4)")
    private Integer year;

    @Column(name = "MONTOBS", nullable = false, columnDefinition = "NUMBER(16,2)")
    private BigDecimal amountBs;

    @Column(name = "TIPO", nullable = true)
    private String type;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", nullable = false, updatable = false, insertable = true)
    private com.encens.khipus.model.admin.Company company;

    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    @Column(name = "COD_GRU", insertable = false, updatable = false, nullable = true)
    private String groupCode;

    @Column(name = "CUENTA", insertable = true, updatable = true, nullable = true)
    private String costAccount;

    @Column(name = "NO_CIA", insertable = false, updatable = false, nullable = true)
    @Length(max = 2)
    private String companyNumber;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumns({
            @JoinColumn(name = "NO_CIA", referencedColumnName = "NO_CIA"),
            @JoinColumn(name = "COD_GRU", referencedColumnName = "COD_GRU")
    })
    private Group group;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumns({
            @JoinColumn(name = "NO_CIA", referencedColumnName = "NO_CIA"),
            @JoinColumn(name = "CUENTA", referencedColumnName = "CUENTA")
    })
    private CashAccount cashAccount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getAmountBs() {
        return amountBs;
    }

    public void setAmountBs(BigDecimal amountBs) {
        this.amountBs = amountBs;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getCostAccount() {
        return costAccount;
    }

    public void setCostAccount(String costAccount) {
        this.costAccount = costAccount;
    }

    public CashAccount getCashAccount() {
        return cashAccount;
    }

    public void setCashAccount(CashAccount cashAccount) {
        this.cashAccount = cashAccount;
    }
}
