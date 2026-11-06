package com.fulllock.app;
import java.lang.ref.WeakReference;
/** One verified detail visit, retained across temporary screens but never shared with another visit. */
public final class DetailVisitBinding<M> {
 private WeakReference<Object> owner=new WeakReference<>(null),request=new WeakReference<>(null);private M model;
 public void remember(Object owner,Object request,M model){this.owner=new WeakReference<>(owner);this.request=new WeakReference<>(request);this.model=model;}
 public M restore(Object owner,Object request){return owner!=null&&this.owner.get()==owner&&this.request.get()==request?model:null;}
 public void forget(Object owner){if(this.owner.get()==owner)clear();}
 public void clear(){owner.clear();request.clear();model=null;}
}
