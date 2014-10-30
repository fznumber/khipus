package com.encens.khipus.model.production;


import com.encens.khipus.model.BaseModel;

import javax.persistence.*;

@TableGenerator(name = "GestionTax_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "gestionimpuesto",
        allocationSize = 10)

@Entity
@Table(name = "gestionimpuesto")
public class GestionTax implements BaseModel {

    @Id
    @Column(name = "idgestionimpuesto", columnDefinition = "NUMBER(24)", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "GestionTax_Generator")
    private Long id;

    //@Column(name = )

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    }
