package com.fulllock.app;

import java.io.*;
import java.math.BigDecimal;
import java.nio.*;
import java.nio.charset.*;
import java.text.Normalizer;
import java.util.*;
import java.util.zip.*;

/** Public, data-only FLP format (imports v1/v2/v3, exports v3). CRC detects corruption, not authorship. */
public final class ProfileFile {
    public static final int MAX_FILE=1048576,MAX_RAW=262144;
    private static final byte[] MAGIC="FLP-PHTM".getBytes(StandardCharsets.US_ASCII);
    public static final Map<String,Object> DEFAULTS;
    static {
        LinkedHashMap<String,Object> defaults=new LinkedHashMap<>();
        defaults.put("price",true);defaults.put("min","100");defaults.put("max","2000");defaults.put("formula",0);defaults.put("fee",20);
        defaults.put("originOn",false);defaults.put("origins","");defaults.put("destOn",false);defaults.put("destinations","");
        defaults.put("excludeOriginOn",false);defaults.put("excludedOrigins","");defaults.put("excludeDestOn",false);defaults.put("excludedDestinations","");
        defaults.put("type",0);defaults.put("bulk",false);defaults.put("themeColor",0xFFF16623);defaults.put("invertText",false);defaults.put("serviceMask",1);defaults.put("rootPayment", "");defaults.put("rootPricing", "");defaults.put("rootServices", "");
        defaults.put("rootDistanceOn",false);defaults.put("rootDistanceMin","0");defaults.put("rootDistanceMax","100");
        defaults.put("rootRateOn",false);defaults.put("rootRateMin","0");defaults.put("rootRateMax","30");
        defaults.put("rootPriority","");defaults.put("rootPayOrder","เงินสด,เครดิต");defaults.put("rootPriceOrder","ส่งปกติ,ส่งด่วนพิเศษ,ส่งประหยัด");
        defaults.put("rootServiceOrder","");defaults.put("rootPrioritySchema",2);defaults.put("rootServicesOn",false);defaults.put("rootPriorityOn",false);defaults.put("rootRepeatSeconds",0);defaults.put("rootPickupOn",false);defaults.put("rootPickupMin","0");defaults.put("rootPickupMax","100");defaults.put("rootOverlayBorder",0xFFF16623);defaults.put("rootOverlayBackground",0xFFFFFFFF);defaults.put("rootOverlayText",0xFF20262E);defaults.put("rootOverlayRgb",false);defaults.put("rootOverlayPulse",true);defaults.put("rootOverlayPulseMs",500);defaults.put("rootRateIncludePickup",false);defaults.put("rootNoRepeat",false);defaults.put("rootRepeatOn",true);defaults.put("rootDetailReturnOn",true);defaults.put("rootDetailReturnSeconds",3);DEFAULTS=Collections.unmodifiableMap(defaults);
    }
    public static final class Data {
        public final String name;public final LinkedHashMap<String,Object> values;
        public Data(String name,Map<String,Object> values){this.name=name(name);this.values=new LinkedHashMap<>(DEFAULTS);this.values.putAll(values);if(!values.containsKey("rootRepeatOn"))this.values.put("rootRepeatOn",!Boolean.TRUE.equals(values.get("rootNoRepeat")));this.values.put("rootNoRepeat",!Boolean.TRUE.equals(this.values.get("rootRepeatOn")));if(!values.containsKey("rootServicesOn"))this.values.put("rootServicesOn",!String.valueOf(this.values.get("rootServices")).isBlank());if(!values.containsKey("rootPriorityOn"))this.values.put("rootPriorityOn",!String.valueOf(this.values.get("rootPriority")).isBlank());if(!values.containsKey("rootPrioritySchema")||Integer.valueOf(1).equals(values.get("rootPrioritySchema"))){if(this.values.get("rootPriority") instanceof String&&this.values.get("rootPayOrder") instanceof String)this.values.put("rootPriority",RootRules.migrateLegacyPriority((String)this.values.get("rootPriority"),(String)this.values.get("rootPayOrder")));this.values.put("rootPrioritySchema",2);}if(Integer.valueOf(3).equals(this.values.get("serviceMask")))this.values.put("serviceMask",1);this.values.put("rootPickupMin","0");validate(this.values);this.values.put("bulk",false);}
    }
    public static String name(String text){String value=Normalizer.normalize(text.trim(),Normalizer.Form.NFC);if(value.isEmpty()||value.codePointCount(0,value.length())>60||value.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("ชื่อโปรไฟล์ไม่ถูกต้อง");return value;}
    private static void validate(Map<String,Object> values){
        if(!values.keySet().equals(DEFAULTS.keySet()))throw new IllegalArgumentException("ข้อมูลโปรไฟล์ไม่ถูกต้อง");
        for(var entry:DEFAULTS.entrySet()){Object value=values.get(entry.getKey());if(value==null||value.getClass()!=entry.getValue().getClass())throw new IllegalArgumentException("ข้อมูลโปรไฟล์ไม่ถูกต้อง");}
        for(String key:new String[]{"min","max","rootDistanceMin","rootDistanceMax","rootRateMin","rootRateMax","rootPickupMin","rootPickupMax"}){String money=(String)values.get(key);if(!money.matches("[0-9]+(\\.[0-9]{1,2})?"))throw new IllegalArgumentException("ช่วงราคาไม่ถูกต้อง");}
        if(new BigDecimal((String)values.get("min")).compareTo(new BigDecimal((String)values.get("max")))>0)throw new IllegalArgumentException("ช่วงราคาไม่ถูกต้อง");
        for(String prefix:new String[]{"rootDistance"})if(new BigDecimal((String)values.get(prefix+"Min")).compareTo(new BigDecimal((String)values.get(prefix+"Max")))>0)throw new IllegalArgumentException("ช่วงค่าไม่ถูกต้อง");
        com.fulllock.app.RootRules.validate(values);
        range(values,"rootOverlayPulseMs",500,5000);range(values,"rootRepeatSeconds",0,20);range(values,"rootDetailReturnSeconds",0,20);range(values,"rootPrioritySchema",2,2);range(values,"formula",0,3);range(values,"fee",0,100);range(values,"type",0,2);range(values,"serviceMask",1,2);
        for(String key:new String[]{"rootOverlayBorder","rootOverlayBackground","rootOverlayText"})if(((Integer)values.get(key)>>>24)!=255)throw new IllegalArgumentException("สีไม่ถูกต้อง");
        if(((Integer)values.get("themeColor")>>>24)!=255)throw new IllegalArgumentException("สีโปรไฟล์ไม่ถูกต้อง");
    }
    private static void range(Map<String,Object> values,String key,int min,int max){int n=(Integer)values.get(key);if(n<min||n>max)throw new IllegalArgumentException("ค่าโปรไฟล์ไม่ถูกต้อง");}
    private static void portable(Map<String,Object> values){for(Object value:values.values())if(value instanceof String&&((String)value).getBytes(StandardCharsets.UTF_8).length>16384)throw new IllegalArgumentException("รายชื่อพื้นที่ยาวเกินไป");for(String key:new String[]{"min","max"})if(!((String)values.get(key)).matches("[0-9]{1,10}(\\.[0-9]{1,2})?"))throw new IllegalArgumentException("ช่วงราคายาวเกินไป");}
    public static byte[] encode(Data data)throws IOException {
        validate(data.values);portable(data.values);
        boolean extended=DEFAULTS.entrySet().stream().anyMatch(e->e.getKey().startsWith("root")&&!e.getValue().equals(data.values.get(e.getKey())));
        ByteArrayOutputStream rawBuffer=new ByteArrayOutputStream();DataOutputStream raw=new DataOutputStream(rawBuffer);raw.writeShort((extended?DEFAULTS.size():18)+1);record(raw,1,data.name);int id=10;for(String key:DEFAULTS.keySet()){if(extended||!key.startsWith("root"))record(raw,id,(key.equals("bulk")?false:data.values.get(key)));id++;}raw.flush();byte[] bytes=rawBuffer.toByteArray();if(bytes.length>MAX_RAW)throw new IOException("โปรไฟล์ใหญ่เกินไป");
        ByteArrayOutputStream compressed=new ByteArrayOutputStream();try(DeflaterOutputStream zip=new DeflaterOutputStream(compressed)){zip.write(bytes);}byte[] payload=compressed.toByteArray();CRC32 crc=new CRC32();crc.update(bytes);
        ByteArrayOutputStream file=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(file);out.write(MAGIC);out.writeByte(extended?3:2);out.writeInt(payload.length);out.writeInt(bytes.length);out.writeInt((int)crc.getValue());out.write(payload);return file.toByteArray();
    }
    private static void record(DataOutputStream out,int id,Object value)throws IOException {out.writeByte(id);if(value instanceof String){byte[] text=((String)value).getBytes(StandardCharsets.UTF_8);out.writeByte(1);out.writeInt(text.length);out.write(text);}else if(value instanceof Boolean){out.writeByte(2);out.writeInt(1);out.writeByte((Boolean)value?1:0);}else{out.writeByte(3);out.writeInt(4);out.writeInt((Integer)value);}}
    public static Data decode(byte[] file)throws IOException {
        try{
            if(file.length<21||file.length>MAX_FILE)throw new IOException("ขนาดไฟล์ไม่ถูกต้อง");DataInputStream in=new DataInputStream(new ByteArrayInputStream(file));byte[] magic=new byte[8];in.readFully(magic);if(!Arrays.equals(magic,MAGIC))throw new IOException("ไฟล์นี้ไม่ใช่โปรไฟล์ Gozza Lalamove");int format=in.readUnsignedByte();if(format!=1&&format!=2&&format!=3)throw new IOException("โปรไฟล์นี้ต้องใช้แอปรุ่นใหม่กว่า");int packed=in.readInt(),length=in.readInt();long checksum=Integer.toUnsignedLong(in.readInt());if(packed!=file.length-21||length<2||length>MAX_RAW)throw new IOException("ขนาดไฟล์ไม่ถูกต้อง");
            Inflater inflater=new Inflater();ByteArrayOutputStream raw=new ByteArrayOutputStream();try{inflater.setInput(file,21,packed);byte[] buffer=new byte[4096];while(!inflater.finished()){int count=inflater.inflate(buffer);if(count==0)throw new IOException("ข้อมูลไฟล์ไม่ครบ");if(raw.size()+count>length)throw new IOException("โปรไฟล์ใหญ่เกินไป");raw.write(buffer,0,count);}if(inflater.getRemaining()!=0||raw.size()!=length)throw new IOException("ข้อมูลไฟล์ไม่ถูกต้อง");}finally{inflater.end();}
            byte[] bytes=raw.toByteArray();CRC32 crc=new CRC32();crc.update(bytes);if(crc.getValue()!=checksum)throw new IOException("ไฟล์เสียหาย");
            DataInputStream input=new DataInputStream(new ByteArrayInputStream(bytes));int count=input.readUnsignedShort();if(count<1||count>DEFAULTS.size()+1)throw new IOException("ข้อมูลโปรไฟล์ไม่ถูกต้อง");Map<String,Object> values=new LinkedHashMap<>();Set<Integer> ids=new HashSet<>();String name=null;List<String> keys=new ArrayList<>(DEFAULTS.keySet());
            for(int i=0;i<count;i++){int id=input.readUnsignedByte(),type=input.readUnsignedByte(),size=input.readInt();if(!ids.add(id)||size<0||size>16384||size>input.available())throw new IOException("ข้อมูลโปรไฟล์ไม่ถูกต้อง");Object value;if(type==1){byte[] text=new byte[size];input.readFully(text);value=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(text)).toString();}else if(type==2&&size==1){int flag=input.readUnsignedByte();if(flag>1)throw new IOException("ข้อมูลโปรไฟล์ไม่ถูกต้อง");value=flag==1;}else if(type==3&&size==4)value=input.readInt();else throw new IOException("ชนิดข้อมูลไม่ถูกต้อง");
                if(id==1&&value instanceof String)name=(String)value;else if(id>=10&&id<10+keys.size()&&(format==3||(format==2&&id<28)||(format==1&&id<27)))values.put(keys.get(id-10),value);else throw new IOException("ข้อมูลโปรไฟล์ที่ยังไม่รองรับ");}
            if(input.available()!=0||name==null)throw new IOException("ข้อมูลไฟล์ไม่ครบ");Data result=new Data(name,values);portable(result.values);return result;
        }catch(DataFormatException|IllegalArgumentException e){throw new IOException("ข้อมูลโปรไฟล์ไม่ถูกต้อง",e);}
    }
    public static byte[] read(InputStream input)throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1){if(out.size()+n>MAX_FILE)throw new IOException("ไฟล์ใหญ่เกินไป");out.write(buffer,0,n);}return out.toByteArray();}
}
