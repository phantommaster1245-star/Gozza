package com.fulllock.app;
import java.util.*;
/** Conservative, job-only retention policy. Unknown or non-clearable notifications are never victims. */
public final class NotificationRetention {
 public static final int LIMIT=15;public static final String DRIVER="com.lalamove.global.driver.sea";
 public record Item(String pkg,String key,long time,String title,String text,boolean ongoing,boolean summary,boolean clearable){}
 public static boolean eligible(Item item){if(item==null||!DRIVER.equals(item.pkg())||item.key()==null||item.key().isEmpty()||item.time()<=0||!item.clearable()||item.ongoing()||item.summary())return false;JobParser.Job job=JobParser.notificationJob(item.title(),item.text(),false,false);return job!=null&&Set.of("ขนส่งสินค้า","รับ-ส่งผู้โดยสาร").contains(job.category());}
 public static List<Item> victims(List<Item> active,String protectedKey,long protectedTime){Map<String,Item> unique=new HashMap<>();for(Item i:active)if(eligible(i))unique.merge(i.key(),i,(a,b)->a.time()>=b.time()?a:b);List<Item> jobs=new ArrayList<>(unique.values());jobs.sort(Comparator.comparingLong(Item::time).thenComparing(Item::key));int remove=Math.max(0,jobs.size()-LIMIT);List<Item> out=new ArrayList<>();for(Item i:jobs){if(out.size()>=remove)break;if(i.key().equals(protectedKey)&&i.time()==protectedTime)continue;out.add(i);}return out;}
}
