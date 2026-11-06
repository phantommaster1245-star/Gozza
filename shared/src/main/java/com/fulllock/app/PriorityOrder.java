package com.fulllock.app;
import java.util.*;
public final class PriorityOrder {
 public static List<String> mergeActive(List<String> stored,List<String> active){Set<String> keys=new HashSet<>();for(String item:active)keys.add(item.split(":")[0]);List<String> out=new ArrayList<>();int next=0;for(String item:stored){if(keys.contains(item.split(":")[0])){if(next<active.size())out.add(active.get(next++));}else out.add(item);}while(next<active.size())out.add(active.get(next++));return out;}
 public static List<String> move(List<String> input,String from,String target,boolean after){List<String> out=new ArrayList<>(input);if(from.equals(target))return out;String moved=null;for(String s:out)if(s.startsWith(from+":")){moved=s;break;}if(moved==null)return out;int to=-1;for(int i=0;i<out.size();i++)if(out.get(i).startsWith(target+":")){to=i;break;}if(to<0)return out;out.remove(moved);for(int i=0;i<out.size();i++)if(out.get(i).startsWith(target+":")){out.add(i+(after?1:0),moved);break;}return out;}
}
