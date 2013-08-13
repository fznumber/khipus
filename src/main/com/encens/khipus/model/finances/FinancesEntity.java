package com.encens.khipus.model.finances;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.UpperCaseStringListener;
import com.encens.khipus.util.Constants;
import org.hibernate.validator.Length;

import javax.persistence.*;

/**
 * FinancesEntity
 *
 * @author
 * @version 2.0
 */
@NamedQueries({
        @NamedQuery(name = "FinancesEntity.countByAcronym", query = "select count(fe) from FinancesEntity fe where lower(fe.acronym)=lower(:acronym)"),
        @NamedQuery(name = "FinancesEntity.countByAcronymAndEntity", query = "select count(fe) from FinancesEntity fe where lower(fe.acronym)=lower(:acronym) and fe.id<>:financesEntityId")
})

@SequenceGenerator(name = "FinancesEntity.sequenceGenerator", sequenceName = Constants.FINANCES_SCHEMA + ".sf_entidad")
@Entity
@EntityListeners(UpperCaseStringListener.class)
@Table(name = "SF_ENTIDADES", schema = Constants.FINANCES_SCHEMA)
public class FinancesEntity implements BaseModel {

    @Id
    @Column(name = "COD_ENTI", length = 6, nullable = false, updatable = false)
    @GeneratedValue(generator = "FinancesEntity.sequenceGenerator")
    private String id;

    //todo This attribute must be use for readonly
    @Column(name = "COD_ENTI", insertable = false, updatable = false)
    private String code;

    @Column(name = "NIT", length = 20)
    @Length(max = 20)
    private String nitNumber;

    @Column(name = "CI", length = 20)
    @Length(max = 20)
    private String idNumber;

    @Column(name = "RAZON_SOCIAL", length = 100)
    @Length(max = 100)
    private String acronym;

    @Column(name = "DIRECCION", length = 100)
    @Length(max = 100)
    private String mainAddress;

    @Column(name = "DIRECCION1", length = 100)
    @Length(max = 100)
    private String secondaryAddress;

    @Column(name = "LUGAR", length = 100)
    @Length(max = 100)
    private String place;

    @Column(name = "TELEFONO", length = 30)
    @Length(max = 30)
    private String phoneNumber;

    @Column(name = "FAX", length = 30)
    @Length(max = 30)
    private String faxNumber;

    @Column(name = "CASILLA", length = 10)
    @Length(max = 10)
    private String postOfficeBox;

    @Column(name = "RESPONSABLE", length = 100)
    @Length(max = 100)
    private String responsible;

    @Column(name = "OBS", length = 240)
    @Length(max = 240)
    private String observations;

    @Column(name = "ESTADO", length = 3)
    @Enumerated(EnumType.STRING)
    private FinancesEntityState state;

    @Column(name = "MOTIVO_BLOQUEO", length = 100)
    @Length(max = 100)
    private String blockReason;

    @Version
    @Column(name = "VERSION")
    private long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNitNumber() {
        return nitNumber;
    }

    public void setNitNumber(String nitNumber) {
        this.nitNumber = nitNumber;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(String mainAddress) {
        this.mainAddress = mainAddress;
    }

    public String getSecondaryAddress() {
        return secondaryAddress;
    }

    public void setSecondaryAddress(String secondaryAddress) {
        this.secondaryAddress = secondaryAddress;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFaxNumber() {
        return faxNumber;
    }

    public void setFaxNumber(String faxNumber) {
        this.faxNumber = faxNumber;
    }

    public String getPostOfficeBox() {
        return postOfficeBox;
    }

    public void setPostOfficeBox(String postOfficeBox) {
        this.postOfficeBox = postOfficeBox;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public FinancesEntityState getState() {
        return state;
    }

    public void setState(FinancesEntityState state) {
        this.state = state;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
