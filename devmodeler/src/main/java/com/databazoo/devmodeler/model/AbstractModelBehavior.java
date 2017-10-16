package com.databazoo.devmodeler.model;

abstract class AbstractModelBehavior<T extends IModelBehavior> implements IModelBehavior<T> {

    protected T valuesForEdit;

    protected boolean isDropped = false;
    protected boolean isNew = false;

    @Override
    public T getValuesForEdit(){
        return valuesForEdit;
    }

    @Override
    public void setValuesForEdit(T behavior){
        valuesForEdit = behavior;
    }

    @Override
    public boolean isDropped(){
        return isDropped;
    }

    @Override
    public void setDropped(){
        isDropped = true;
    }

    @Override
    public void setNotDropped(){
        isDropped = false;
    }

    @Override
    public boolean isNew(){
        return isNew;
    }

    @Override
    public void setNew(){
        isNew = true;
    }

    @Override
    public void setNotNew(){
        isNew = false;
    }


}
