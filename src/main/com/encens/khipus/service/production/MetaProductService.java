package com.encens.khipus.service.production;

import com.encens.khipus.model.production.MetaProduct;

import javax.ejb.Local;

/**
 * Created with IntelliJ IDEA.
 * User: Ariel Siles Encinas
 */
@Local
public interface MetaProductService {
    MetaProduct find(long id);
}
