package com.fulllock.probe;
import android.view.View;import android.graphics.Rect;
/** Reads attached edge cards without treating completely offscreen items as visible/actionable. */
final class CardViewport {
 static View scope(View view,int origin,int destination,int price){View recycler=view;for(int i=0;recycler!=null&&i<16&&!recycler.getClass().getName().contains("RecyclerView");i++)recycler=recycler.getParent() instanceof View?(View)recycler.getParent():null;if(recycler==null||!recycler.getClass().getName().contains("RecyclerView"))return null;for(int depth=0;view!=null&&depth<12;depth++){if(view==recycler)return null;if(origin!=0&&destination!=0&&price!=0&&view.findViewById(origin)!=null&&view.findViewById(destination)!=null&&view.findViewById(price)!=null){Rect r=new Rect();return view.isShown()&&view.getGlobalVisibleRect(r)?view:null;}view=view.getParent() instanceof View?(View)view.getParent():null;}return null;}
 static Rect bounds(View view){Rect r=new Rect();if(!view.getGlobalVisibleRect(r)){int[] at=new int[2];view.getLocationOnScreen(at);r.set(at[0],at[1],at[0]+view.getWidth(),at[1]+view.getHeight());}return r;}
 static boolean inside(View child,View root){for(int i=0;child!=null&&i<16;i++){if(child==root)return true;child=child.getParent() instanceof View?(View)child.getParent():null;}return false;}
 static boolean actionable(View root,int... ids){for(int id:ids){View view=root.findViewById(id);if(view==null||!view.getGlobalVisibleRect(new Rect()))return false;}return true;}
}
