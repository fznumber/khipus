package com.encens.khipus.model.customers;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.warehouse.ProductItem;
import com.encens.khipus.util.Constants;
import org.hibernate.validator.Length;

import javax.persistence.*;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */

/*@TableGenerator(name = "ArticleOrder_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "PEDIDOS",
        allocationSize = 10)

@Entity
@Table(name = "ARTICULOS_PEDIDO",schema = Constants.CASHBOX_SCHEMA)
//@Filter(name = "companyFilter")
//@EntityListeners(CompanyListener.class)*/
public class ArticleOrder {
    /*@Column(name = "PEDIDO", nullable = false, length = 10)
    @Length(max = 10)
    private String order;

    @Column(name = "ID", nullable = false, length = 20)
    @Length(max = 20)
    private String orderID;

    @Column(name = "ID1", nullable = false, length = 10)
    @Length(max = 10)
    private Integer orderID1;

*//*    @Column(name = "ID_CUENTA", nullable = false, length = 10)
    private Integer idAccount;

    @Column(name = "COD_ART", nullable = false, length = 6)
    @Length(max = 6)
    private String codArt;

    @Column(name = "NO_CIA", nullable = false, length = 2)
    @Length(max = 2)
    private String companyNumber;*//*

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumns({
            @JoinColumn(name = "PEDIDO", referencedColumnName = "PEDIDO"),
            @JoinColumn(name = "ID", referencedColumnName = "ID"),
            @JoinColumn(name = "ID1", referencedColumnName = "ID1")
    })
    private CustomerOrder customerOrder;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumns({
            @JoinColumn(name = "ID_CUENTA", referencedColumnName = "ID_CUENTA"),
            @JoinColumn(name = "COD_ART", referencedColumnName = "COD_ART"),
            @JoinColumn(name = "NO_CIA", referencedColumnName = "NO_CIA")
    })
    private AccountItem accountItem;

    @Column(name = "CANTIDAD", nullable = true)
    private Integer amount;

    @Column(name = "COD_ALM", nullable = true )
    private String codWarehouse;

    @Column(name = "PRECIO",nullable = true )
    private Double price;

    @Column(name = "REPOSICION",nullable = true)
    private Integer reposicion;

    @Column(name = "TOTAL",nullable = true)
    private Double total;

    @Column(name = "PROMOCION",nullable = true)
    private Integer promotion;

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public Integer getOrderID1() {
        return orderID1;
    }

    public void setOrderID1(Integer orderID1) {
        this.orderID1 = orderID1;
    }

    *//*public Integer getIdAccount() {
        return idAccount;
    }

    public void setIdAccount(Integer idAccount) {
        this.idAccount = idAccount;
    }

    public String getCodArt() {
        return codArt;
    }

    public void setCodArt(String codArt) {
        this.codArt = codArt;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }*//*

    public CustomerOrder getCustomerOrder() {
        return customerOrder;
    }

    public void setCustomerOrder(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
    }

    public AccountItem getAccountItem() {
        return accountItem;
    }

    public void setAccountItem(AccountItem accountItem) {
        this.accountItem = accountItem;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getCodWarehouse() {
        return codWarehouse;
    }

    public void setCodWarehouse(String codWarehouse) {
        this.codWarehouse = codWarehouse;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getReposicion() {
        return reposicion;
    }

    public void setReposicion(Integer reposicion) {
        this.reposicion = reposicion;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Integer getPromotion() {
        return promotion;
    }

    public void setPromotion(Integer promotion) {
        this.promotion = promotion;
    }
*/}
