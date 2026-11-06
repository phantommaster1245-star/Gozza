package com.fulllock.app;
import java.util.*;

/** Session-only list. A timed preview never marks jobs read. */
final class FloatingJobs {
    enum Mode { COLLAPSED, PREVIEW, PINNED }
    static final class Item {
        final String id,key,title,text;
        final long postTime;
        final JobParser.Job job;
        boolean matches;
        String extraSummary;
        java.math.BigDecimal commissionReduction;boolean allowMerge=true;boolean available;String notificationKey="",screenKey="",notificationTitle="",notificationText="";long notificationTime;
        Item(String key,long postTime,String title,String text,JobParser.Job job,boolean matches,boolean available) {
            this(key,postTime,title,text,job,matches,available,"");
        }
        Item(String key,long postTime,String title,String text,JobParser.Job job,boolean matches,boolean available,String extraSummary) {
            this.key=key;this.postTime=postTime;this.title=title;this.text=text;this.job=job;this.matches=matches;this.available=available;
            this.extraSummary=extraSummary==null?"":extraSummary;
            id=key+"\n"+title+"\n"+text;
            if(key.startsWith("screen:"))screenKey=key.substring(7);else {notificationKey=key;notificationTime=postTime;notificationTitle=title;notificationText=text;}
        }
    }
    final List<Item> items=new ArrayList<>();
    int unread;
    Mode mode=Mode.COLLAPSED;
    void put(Item item,boolean visible) {
        Item merged=null;for(Item old:items)if(!item.screenKey.isEmpty()&&item.screenKey.equals(old.screenKey)||!item.notificationKey.isEmpty()&&item.notificationKey.equals(old.notificationKey)&&item.notificationTime==old.notificationTime&&(!old.screenKey.isEmpty()||!item.screenKey.isEmpty())){merged=old;break;}if(merged==null)merged=mergeCandidate(item);boolean existing;
        if(merged!=null){int at=items.indexOf(merged);copySources(merged,item);items.set(at,item);if(mode==Mode.PINNED)unread=0;return;}
        existing=items.removeIf(old->old.id.equals(item.id));
        items.add(0,item);if(items.size()>10)items.remove(items.size()-1);
        if(mode==Mode.PINNED)unread=0;
        else {if(!existing)unread++;if(visible)mode=Mode.PREVIEW;}
    }
    static boolean sameJob(Item a,Item b){return a.job!=null&&b.job!=null&&a.job.type().equals("ขนส่งทันที")&&a.job.type().equals(b.job.type())&&a.job.category().equals(b.job.category())&&a.job.origin().equals(b.job.origin())&&a.job.destination().equals(b.job.destination())&&a.job.fare().compareTo(b.job.fare())==0&&a.job.tip().compareTo(b.job.tip())==0&&Math.abs(a.postTime-b.postTime)<=30000;}
    Item mergeCandidate(Item item){if(!item.allowMerge)return null;Item found=null;for(Item old:items){if(!old.allowMerge)continue;boolean cross=item.key.startsWith("screen:")?!old.notificationKey.isEmpty()&&old.screenKey.isEmpty():!old.screenKey.isEmpty()&&old.notificationKey.isEmpty();if(cross&&sameJob(old,item)){if(found!=null)return null;found=old;}}return found;}
    static void copySources(Item old,Item item){if(item.screenKey.isEmpty()&&!old.screenKey.isEmpty())item.matches=old.matches;if(item.commissionReduction==null)item.commissionReduction=old.commissionReduction;if(item.notificationKey.isEmpty()){item.notificationKey=old.notificationKey;item.notificationTime=old.notificationTime;item.notificationTitle=old.notificationTitle;item.notificationText=old.notificationText;}if(item.screenKey.isEmpty())item.screenKey=old.screenKey;if(item.extraSummary.isEmpty()&&!old.extraSummary.isEmpty())item.extraSummary=old.extraSummary;}
    void open(){mode=Mode.PINNED;unread=0;}
    void collapse(){mode=Mode.COLLAPSED;}
    void expire(){if(mode==Mode.PREVIEW)collapse();}
    void reset(){items.clear();unread=0;collapse();}
    List<Item> displayed(boolean showUnavailable,boolean preview){List<Item> visible=new ArrayList<>();for(int i=0;i<items.size()&&(!preview||i==0);i++){Item item=items.get(i);if(showUnavailable||item.available)visible.add(item);}return visible;}
    void removed(String key,long time){for(Item item:items)if(item.notificationKey.equals(key)&&item.notificationTime<=time&&item.screenKey.isEmpty())item.available=false;}
}
