import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringReader;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

class Term implements Comparable<Term>{
  private String fullname; 
  private ArrayList<String> components;
  
  Term(String name){
    this.fullname = name;
    
    components = new ArrayList<String>();
    String [] strs = fullname.split(" ");
    for(String s : strs)
      components.add(s.trim());
  }
  
  public String getName(){
    return this.fullname;
  }
  
  public ArrayList<String> getComponents(){
    return this.components;
  }
  
  public boolean hasComponents(){
    return this.components.size() > 1;
  }
  
  @Override
  public int compareTo(Term o) {
    return this.fullname.compareTo(o.fullname);
  }
  
  public boolean equals(Object o){
    Term t = (Term)o;
    return this.fullname.equals(t.fullname);
  }
  
  public int hashCode(){
    return this.fullname.hashCode();
  }
  
}

class MyAnnotation{
  int begin, end;
  String sentenceID;
  String name;
  
  public void setBegin(int begin){
    this.begin = begin;
  }
  public void setEnd(int end){
    this.end = end;
  }
  public void setSentenceID(String id){
    sentenceID = id;
  }
  public void setName(String name){
    this.name = name;
  }
}

class PosTag {

  private StanfordCoreNLP pipeline;
  private Map<String, String> posDic = new HashMap<String, String>();
  
  public Map<String, String> getPosDic(){
    return posDic;
  }
  
  public PosTag(){
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos");
    pipeline = new StanfordCoreNLP(props);
  }
  

  public void getGeneSpans(String text) {
    posDic.clear();
    
    Annotation document = new Annotation(text);
    pipeline.annotate(document);
    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
    for (CoreMap sentence : sentences) {
      for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
        String pos = token.get(PartOfSpeechAnnotation.class);
        if (pos.startsWith("NN")) {
          posDic.put(token.toString(), pos);
        } 
        
      }
    }
  }
  
  
}

class Reg{
  private String DIC_FILE = "/home/xuke/workspace/hw1-kex/src/main/resources/dictionary.txt";
  private String INPUT = "/home/xuke/workspace/hw1-kex/src/main/resources/hw1.in";
  private String OUTPUT = "/home/xuke/workspace/hw1-kex/src/main/resources/hw1out.txt";
  private String STD_OUTPUT = "/home/xuke/workspace/hw1-kex/src/main/resources/sampleout.txt";
  private String DIFF = "/home/xuke/workspace/hw1-kex/src/main/resources/diff.txt";
  
  private String FORBID1 = "/home/xuke/workspace/hw1-kex/src/main/resources/oxoutout.txt";
  private String FORBID2 = "/home/xuke/workspace/hw1-kex/src/main/resources/GoneWithTheWinduu.txt";
  private String FORBID3 = "/home/xuke/workspace/hw1-kex/src/main/resources/ncet4.txt";
  private String FORBID4 = "/home/xuke/workspace/hw1-kex/src/main/resources/engout.txt";
  
  private String FULDIC = "/home/xuke/workspace/hw1-kex/src/main/resources/fulldictionary.txt";
  
  private Set<String> singleDic = new HashSet<String>();
  private Set<String> dictionary = new HashSet<String>();
  private Set<String> commonWords = new HashSet<String>();
  private ArrayList<MyAnnotation> entities = new ArrayList<MyAnnotation>();
  
  public void pipeline(){
    loadDic();
  //  loadForbiddenDic(FORBID1);
    loadForbiddenDic(FORBID2);
    loadForbiddenDic(FORBID3);
    loadForbiddenDic(FORBID4);
    loadForbiddenDic(FULDIC, dictionary);
    
    rec();
    genRes();
    
    // output difference between my ouput and standard output
    genDiff();
  }
  
  void loadDic(){
    try{
      Scanner sc = new Scanner(new File(DIC_FILE));
      while(sc.hasNextLine()){
        String line = sc.nextLine();
        String [] terms  = line.split(" ");
        for(String t : terms)
          dictionary.add(t.toLowerCase());
        
        singleDic.add(terms[0]);
      }
      
      sc.close();
    }
    catch(Exception e){
      
    }
  }
  
  void loadForbiddenDic(String file){
    try{
      Morphology mor = new Morphology();
      Scanner sc = new Scanner(new File(file));
      while(sc.hasNext()){
        String token = sc.next().trim();
         commonWords.add(token);
         commonWords.add(mor.stem(token));
        
      }
      
      sc.close();
    }
    catch(Exception e){
      
    }
  }
  
  void loadForbiddenDic(String file, Set<String> dic){
    try{
      Scanner sc = new Scanner(new File(file));
      while(sc.hasNext()){
        String token = sc.next().trim();
        
        if(!dic.contains(token))
          commonWords.add(token);
      }
      
      sc.close();
    }
    catch(Exception e){
      
    }
  }
  
  boolean isAllLetters(String token){
    for(int i=0; i<token.length(); i++){
      char c = token.charAt(i);
      if(!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')))
        return false;
    }
    return true;
  }
  
  void rec(){
    try{
      Scanner sc = new Scanner(new File(INPUT));
      while(sc.hasNextLine()){
        String line = sc.nextLine();
        parseLine(line);
      }
      
      sc.close();
    }
    catch(Exception e){
      
    }
  }
  
  void parseLine(String line){
    // POS
    PosTag pos = new PosTag();
    pos.getGeneSpans(line);
    
    // tokenize 
    TokenizerFactory<Word> factory = PTBTokenizerFactory.newTokenizerFactory();
    Tokenizer<Word> tokenizer = factory.getTokenizer(new StringReader(line));
    List<Word> words = tokenizer.tokenize();
    
    // stem
    Morphology mor = new Morphology();     
    ArrayList<String> tokens = new ArrayList<String>();
    for(Word word : words){
      tokens.add(word.toString());
    }
    
    
    int i = 1; // token[0] is sentence ID
    int curLen = 0;
    while(i < tokens.size()){
      if(!isValidToken(tokens.get(i)) || 
              (!dictionary.contains(tokens.get(i).toLowerCase()) && !isAllUpperCase(tokens.get(i))) || 
                commonWords.contains(tokens.get(i).toLowerCase()) || 
                commonWords.contains(mor.stem(tokens.get(i).toLowerCase()))){ 
        curLen += tokens.get(i).length();
        i++;
        continue;
      }
      
      boolean multicase = false;
      int j = i + 1;
      for(; j < tokens.size() && 
              isValidToken(tokens.get(j)) && 
               ( dictionary.contains(tokens.get(j).toLowerCase()) || isAllUpperCase(tokens.get(i))) &&
                  !commonWords.contains(tokens.get(j).toLowerCase()) &&
                   !commonWords.contains(mor.stem(tokens.get(j).toLowerCase())); j++)
        multicase = true;
      
      // merge token [i - j)
      StringBuilder name = new StringBuilder();
      int len = 0;
      for(int k = i; k < j; k++){
        len += tokens.get(k).length();
        name.append(tokens.get(k));
        if(k+1 < j)name.append(" ");
      }
      
      // set MyAnnotation
      MyAnnotation ann = new MyAnnotation();
      ann.setSentenceID(tokens.get(0));
      ann.setBegin(curLen);
      ann.setEnd(curLen + len - 1);
      ann.setName(name.toString());
      
      // TODO set common word dictionary for exclusion
      if(multicase || (!multicase && singleDic.contains(ann.name) && !commonWords.contains(ann.name))){
       // judge POS
        if(pos.getPosDic().get(ann.name.trim()) != null)
          entities.add(ann);
      }
      
      curLen += len;
      i = j;
    }
    
  }
  
  boolean isValidToken(String context){
    if(context.matches("[,.?:;'{}!*+-=_]") || 
                 isNumber(context) || hasNoLetter(context) || 
                 (context.charAt(0) == '-' && context.charAt(context.length()-1) == '-'))
      return false;
    return true;
  }

  boolean isNumber(String context){
    for(int i = 0; i < context.length(); i++){
      char c = context.charAt(i);
      if(c > '9' || c < '0')
        return false;
    }
    return true;
  }
  
  boolean hasNoLetter(String context){
    for(int i=0; i<context.length(); i++){
      char c = context.charAt(i);
      if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
        return false;
    }
    
    return true;
  }
  
  boolean isAllUpperCase(String context){
    for(int i=0; i<context.length(); i++){
      char c = context.charAt(i);
      if(!(c >= 'A' && c <= 'Z'))
        return false;
    }
    return true;
  }
  
  void genRes(){
    try{
      PrintWriter pw = new PrintWriter(new File(OUTPUT));
      for(MyAnnotation ann : entities){
        String line = ann.sentenceID + " " + ann.name + " " + ann.begin + " " + ann.end + "\n";
        pw.write(line);
      }
      
      pw.close();
    }
    catch(Exception e){
      
    }
  }
  
  void genDiff(){
    try{
      Set<String> stdout = new HashSet<String>();
      Scanner sc = new Scanner(new File(STD_OUTPUT));
      while(sc.hasNextLine()){
        stdout.add(sc.nextLine().trim());
      }
      sc.close();

      PrintWriter pw = new PrintWriter(new File(DIFF));
      for(MyAnnotation ann : entities){     
        if(!stdout.contains(ann.name.trim()))
          pw.write(ann.name + "\n");
      }
      
      pw.close();
    }
    catch(Exception e){
      
    }
  }
  
}

public class DependencyExample {
/**
* Tokenize a sentence in the argument, and print out the tokens to the console.
*
* @param args
   Set the first argument as the sentence to be tokenized.
*
*/
public static void main(String[] args) {
  Reg r = new Reg();
  r.pipeline();
  /*
  DependencyExample exp = new DependencyExample();
  Scanner sc = null;
  
  try{
    PrintWriter pw = new PrintWriter(new File("/home/xuke/workspace/hw1-kex/src/main/resources/hw1out.txt"));
    
    sc = new Scanner(new File(INPUT));
    int nlines = 0;
    while(sc.hasNextLine()){
      nlines ++;
      String line = sc.nextLine();
      if(line.equals("q"))
        break;
      
      exp.annotateLine(line);
      if(exp.anns.size() != 0){
        for(MyAnnotation ann : exp.anns){
          //System.out.println(ann.sentenceID + " " + ann.name + " " + ann.begin + " " + ann.end);
          pw.write(ann.sentenceID + " " + ann.name + " " + ann.begin + " " + ann.end + "\n");
        }
      }
      
      exp.anns.clear();
    }
    
    System.out.println(nlines);
    pw.close();
    }
    catch(Exception e){
      
    }
    finally{
      if(sc != null)
        sc.close();
    }
    */
}

public static String CORPUS = "/home/xuke/workspace/hw1-kex/src/main/resources/shakespeare.txt";
public static String CORPUS2 = "/home/xuke/workspace/hw1-kex/src/main/resources/GoneWithTheWind.txt";
public static String CORPUS3 = "/home/xuke/workspace/hw1-kex/src/main/resources/ncet4.txt";
public static String CORPUS4 = "/home/xuke/workspace/hw1-kex/src/main/resources/engout.txt";

public static String DIC_FILE = "/home/xuke/workspace/hw1-kex/src/main/resources/dictionary.txt";
public static String DIC_FILE2 = "/home/xuke/workspace/hw1-kex/src/main/resources/sampleout.txt";

public static String CORPUS5 = "/home/xuke/workspace/hw1-kex/src/main/resources/fulldictionary.txt";

public static String INPUT = "/home/xuke/workspace/hw1-kex/src/main/resources/hw1.in";

public Map<String, Term> dictionary;
public Map<String, Integer> commonWords;
public ArrayList<MyAnnotation> anns;

DependencyExample(){
  dictionary = new HashMap<String, Term>();
  loadDictionary(DIC_FILE);
//  loadDictionary(DIC_FILE2);
  
  commonWords = new HashMap<String, Integer>();
  loadCommonWords(CORPUS);
  loadCommonWords(CORPUS2);
  loadCommonWords(CORPUS3);
  loadCommonWords(CORPUS4);
  loadCommonWords(CORPUS5, DIC_FILE);
  
  anns = new ArrayList<MyAnnotation>();
}

private void loadDictionary(String fileName){
  File file = new File(fileName);
  if(!file.exists() || !file.isFile()){
    System.out.println("File not found!");
  }
  
  Scanner sc = null;
  try {
    sc = new Scanner(file);
    while(sc.hasNextLine()){
      String line = sc.nextLine();
      parseLine(line);
    }
    
  } catch (FileNotFoundException e) {
    // file existence has been checked in the assert above
  }
  finally{
    if(sc != null)
      sc.close();
  }
}

public void loadCommonWords(String fileName){
  File file = new File(fileName);
  if(!file.exists() || !file.isFile()){
    System.out.println("File not found!");
  }
  
  int cnt = 0;
  Scanner sc = null;
  try {
    sc = new Scanner(file);
    while(sc.hasNext()){
      String word = sc.next(); 
      
      cnt++;
      parseWord(word);
    }
    
  } catch (FileNotFoundException e) {
    // file existence has been checked in the assert above
  }
  finally{
    if(sc != null)
      sc.close();
  }
  
  System.out.println(cnt + " new words from <" + fileName + "> have been added into common word list");
}

private void loadCommonWords(String fileName, String exclusion){
  Set<String> exwords = new HashSet<String>();
  
  try{
    Scanner ex = new Scanner(new File(exclusion));
    while(ex.hasNextLine()){
      String line = ex.nextLine();
      String [] coms = line.split(" ");
      for(String s : coms)
        exwords.add(s);
    }
    
    ex.close();
    
    Scanner dic = new Scanner(new File(fileName));
    while(dic.hasNext()){
      String word = dic.next();
      if(!exwords.contains(word)){
        int cnt = 1;
        if(commonWords.containsKey(word))
          cnt = commonWords.get(word) + 1;       
        commonWords.put(word, cnt);
      }
    }
    
    dic.close();
  }
  catch(Exception e){
    
  }
}

private void parseLine(String line){
  Term term = new Term(line);
  dictionary.put(line, term);   
}

private void parseWord(String word){
  int cnt = 1;
  if(commonWords.containsKey(word))
    cnt += commonWords.get(word);
  
  commonWords.put(word, cnt);
}

private void annotateLine(String line){
  TokenizerFactory<Word> factory = PTBTokenizerFactory.newTokenizerFactory();
  Tokenizer<Word> tokenizer = factory.getTokenizer(new StringReader(line));
  List<Word> tokens = tokenizer.tokenize();
  
  // get the sentence ID, which is the first substring of line
  String sentenceID = tokens.get(0).toString();
  int idx = 1;
  int passedLen = 0;
  ArrayList<MyAnnotation> eles = new ArrayList<MyAnnotation>();
  
  while(idx < tokens.size()){
    String key = tokens.get(idx).toString().trim();  
    
    Term t = null;
    if(isValidToken(key) && (t = dictionary.get(key)) != null ){
      if(!t.hasComponents()){
        MyAnnotation ann = new MyAnnotation(); 
        ann.setBegin(passedLen);
        passedLen += key.length();
        ann.setEnd(passedLen - 1);
        ann.setSentenceID(sentenceID);
        ann.setName(key);
    
        eles.add(ann);
     
        idx++;
      }else{
        
        boolean multicase = false;
        MyAnnotation ann = new MyAnnotation();
        ann.setBegin(passedLen);
        ArrayList<String> coms = t.getComponents();
           
        StringBuilder name = new StringBuilder(coms.get(0));
        passedLen += coms.get(0).length();
        
        int j = idx + 1;
        for(int i = 1; i < coms.size(); i++, j++)
          if(!(j < tokens.size() && tokens.get(j).toString().equals(coms.get(i))))
            break;
          else{
            multicase = true;
            passedLen += coms.get(i).length();
            name.append(" " + coms.get(i));
          }

        ann.setEnd(passedLen - 1);
        ann.setSentenceID(sentenceID);
        ann.setName(name.toString());
    
        if(multicase)
          eles.add(ann);
        else{
          Integer cnt = commonWords.get(key);
          if(cnt == null || cnt == 0){          
            if(!isNumber(key)){
              eles.add(ann);
            }
          }
        }
        idx = j;
      }
    }else{
      passedLen += key.length();
      idx++;
    } 
  } 

  //merge continuous annotations to one annotation   
  int start = 0;
  while(start < eles.size()){
    int end = start + 1;
    for(; end < eles.size(); end++)
      if(eles.get(end).begin != eles.get(end-1).end + 1 || 
                !eles.get(end).sentenceID.equals(eles.get(start).sentenceID))
        break;
    
    // merge [start, end] => one annotation
    StringBuilder name = new StringBuilder();
    for(int i = start; i < end; i++){
      name.append(eles.get(i).name);
      if(i+1 < end)
        name.append(" ");
    }
    
    // add merged annotation to anns
    MyAnnotation mergedAnn = new MyAnnotation();
    mergedAnn.setBegin(eles.get(start).begin);
    mergedAnn.setEnd(eles.get(end-1).end);
    mergedAnn.setSentenceID(eles.get(start).sentenceID);
    mergedAnn.setName(name.toString());
    
    if(commonWords.get(mergedAnn.name) == null)
      anns.add(mergedAnn);
    
    if(tnum ++ <= 2)
      System.out.println(mergedAnn.name);
    
    start = end;
  }
  
}

private int tnum = 0;

private boolean isValidToken(String context){
  if(context.equals(",") ||
          (context.charAt(0) == '-' && context.charAt(context.length()-1) == '-'))
    return false;
  return true;
}

private boolean isNumber(String context){
  for(int i = 0; i < context.length(); i++){
    char c = context.charAt(i);
    if(c > '9' || c < '0')
      return false;
  }
  return true;
}

}
