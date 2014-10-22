import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Scanner;


public class LM {
	public static Hashtable<Integer,double[]> CENTER_MEAN = new Hashtable<Integer,double[]> () ;
	public static int[] group = new int[2265];
	public static Hashtable<Integer,double[]> docofgroup = new Hashtable<Integer,double[]> () ;
	public static void main(String[] args) throws IOException {
		String base_path = LM.class.getResource("/") .getPath() ;
		String pCorpus = base_path + "Word_Unigram_Xinhua98Upper.txt" ;
		Scanner readfile = new Scanner(new File(pCorpus)) ;
		double p_corpus[] = new double[51253] ;
		int line_index = 0 ;
		String line = null ;
		int[] check_group = new int[2265] ;
		
		
		//p(w|copus)
		while(readfile.hasNext()){
			line = readfile.nextLine();
			String[] spline = line.trim().split(" ") ;
			p_corpus[line_index] = Double.parseDouble(spline[1]) ;
			line_index++ ;
			//System.out.println(line);
		}
		
		readfile.close() ; //close Scanner

		//count all of the word frequent in query
		//c(w|q)
		String Query = base_path + "QUERY_WDID_NEW" ;
		File Query_txt = new File(Query) ;
		String[] Query_name = Query_txt.list() ;
		Hashtable<String,Hashtable<String,Integer>> q_w ;
		q_w = open_exe(Query_name,Query);//算自己出現的word次數
		
		
		//count all of the word frequent in docuement
		//p(w|d)
		String dataset = base_path + "SPLIT_DOC_WDID_NEW" ;
		File docuement = new File(dataset) ; 
		String[] article_name = docuement.list() ;
		Hashtable<String,Hashtable<String,Integer>> d_Wd ;
		d_Wd = open_exe(article_name,dataset);//算自己出現的word次數
		
		//初始文章前10篇為中心點 STEP 1
		
		for(int g=0;g<10;g++){
			CENTER_MEAN.put(g,changeArray(d_Wd.get(article_name[g]))) ;
		}
		
		//(K-means)STEP2
		
		initKmeans(d_Wd,article_name);

		check_group = group ;
		//算word的次數，在Q裡面出現,同時也在D出現。
		group_d_count(article_name,d_Wd);
		qw_map_d(q_w,d_Wd,p_corpus,Query_name,article_name);
		
		
	}



	private static void group_d_count(String[] article_name, Hashtable<String, Hashtable<String, Integer>> d_Wd) {
		
		
		for(int k=0;k<10;k++){
			double total_glength = 0 ;
			double[] total_cwofgroup = new double[51253] ;
			for(int i=0;i<article_name.length;i++){
				if(group[i]==k){
					Hashtable<String, Integer> d_length ;
					d_length = d_Wd.get(article_name[i]) ;
					double tmp[] = changeArray(d_length);
					total_glength = total_glength + d_length.get("total_length") ;
					for(int y=0;y<tmp.length;y++){
						total_cwofgroup[y] = total_cwofgroup[y] + tmp[y] ;
					}
				}
			}
			for(int x=0;x<total_cwofgroup.length;x++){
				total_cwofgroup[x] = total_cwofgroup[x]/total_glength ;
			}
			docofgroup.put(k, total_cwofgroup);
		}
		
	}



	private static void initKmeans(Hashtable<String, Hashtable<String, Integer>> d_Wd,String[] article_name) {
		//d_Wd文章出現的字數和總長度  article_name文章名字
		int check_g[] = new int[2265] ;
		for(int c=0;c<20;c++){
			for(int ug=0;ug<2265;ug++){
				String ug_name =  article_name[ug] ; 
				double[] A = new double[51253];
				double[] B = new double[51253];
				A = changeArray(d_Wd.get(ug_name));
				double cosMax = 0;
				int Max_index = 0 ;
				for(int g=0;g<10;g++){
					B = CENTER_MEAN.get(g) ;//與中心點相比距離

					double temp = cos(A,B) ;
					if(temp>cosMax){
						cosMax = temp ;
						Max_index = g ;//代表與第I群最像  : I = g 
					}
				}	
				group[ug] = Max_index ;
				check_g = group ;
			}
			//重新計算中心點
			Centermean(article_name,d_Wd);
		}
	}



	private static double[] changeArray(Hashtable<String, Integer> hashtable) {
		Enumeration<String> word = hashtable.keys();
		double[] a = new double[51253] ;
		while(word.hasMoreElements()){
			String wordkey = word.nextElement() ;
			if(!wordkey.equals("total_length")){
				a[Integer.parseInt(wordkey)] = hashtable.get(wordkey) ;
			}
		}
		return a ;
	}



	private static void Centermean(String[] article_name, Hashtable<String, Hashtable<String, Integer>> d_Wd) {
		for(int g=0;g<10;g++){
			double[] tmp_mean= new double[51253];
			double tmp[] = new double[51253] ;
			double count = 0 ;
			for(int a=0;a<article_name.length;a++){
				if(group[a]==g){
					count++ ;
					tmp = changeArray(d_Wd.get(article_name[a])) ;
					for(int c=0;c<51253;c++){
						tmp_mean[c] = tmp_mean[c] + tmp[c] ;
					}
				}
			}
			
			tmp_mean = mean(tmp_mean,count);
			CENTER_MEAN.put(g, tmp_mean);
			tmp_mean = CENTER_MEAN.get(g) ;
		}

	}



	private static double[] mean(double[] tmp_mean, double count) {
		for(int c=0;c<51253;c++){
			tmp_mean[c] = tmp_mean[c] / count ;
		}
		return tmp_mean;
	}



	private static double cos(double[] a,double[] b) {
		double cos = 0 ;
		double conduct_g_ug = 0 ;
		double g_square = 0 ;
		double ug_square = 0 ;

		//AB內積 + |A| +|B|
		for(int i=0;i<a.length;i++){
			conduct_g_ug = conduct_g_ug + (a[i] * b[i]) ;
			g_square = g_square + a[i]*a[i] ;
			ug_square = ug_square + b[i]*b[i] ;
		}

		cos = conduct_g_ug / (Math.sqrt(g_square)*Math.sqrt(ug_square));
		return cos ;
		
	}



	private static void qw_map_d(Hashtable<String, Hashtable<String, Integer>> q_w,Hashtable<String,
			Hashtable<String, Integer>> d_Wd, double[] p_corpus, String[] query_name, String[] article_name) throws IOException {
		
		String[] E_list_D = article_name ;
		double[] logP_qord = new double[2265] ; 
		Hashtable<String, Integer> q_d_has = new Hashtable<String, Integer>() ;
		
		//印出
		FileWriter output = new FileWriter("D://IR4_list.txt");
		PrintWriter out = new PrintWriter(output) ;
		
		for(int i=0;i<query_name.length;i++){
			//取得query name
			Hashtable<String, Integer> wf_of_q = q_w.get(query_name[i]) ;//按順序取出query
			Enumeration<String> qkey = null;
			String qword = null ;
			
			for(int j=0;j<article_name.length;j++){
				//取得doc name
				Hashtable<String, Integer> Wdf_of_d = d_Wd.get(article_name[j]) ;
				qkey = wf_of_q.keys();//為了重新指標歸0計數
				int gid = group[j] ;//知道第幾群
				double[] wofgroup = docofgroup.get(gid) ;
				while(qkey.hasMoreElements()){
					//取得query出現的字
					qword = qkey.nextElement() ;
					if(Wdf_of_d.containsKey(qword) && !qword.equals("total_length")){
						//query & doc has same word
						q_d_has.put(qword,Wdf_of_d.get(qword)) ;
					}
				}
				logP_qord[j] = logP_qord_exe(wf_of_q,Wdf_of_d,q_d_has,p_corpus,wofgroup) ;
				q_d_has = new Hashtable<String, Integer>() ;
			}
			
			//由大至小-(select sort)
			int decrease = 0;
			int index = 0;
			double temp = 0;
			String dname = null ;
			
			for(int z=0;z<logP_qord.length;z++){
				temp = logP_qord[decrease];//第一個
				index = decrease;//值的位址
				decrease++ ;
				
				while(decrease<logP_qord.length){
					if(logP_qord[decrease]>temp){
						temp = logP_qord[decrease]; 
						index = decrease ;
						dname = E_list_D[decrease]; //doc檔名
					}
					decrease++;
				}
				logP_qord[index] = logP_qord[z];
				logP_qord[z] = temp ;
				E_list_D[index] = E_list_D[z];
				E_list_D[z] = dname;
				decrease = z+1 ;
			}
			
			
			
			
			/*if(i==2){
				System.out.println() ;
			}*/
			//System.out.print("Query "+(q+1)+"\t"+query_name[q]+" "+article_name.length) ;
			out.print("Query "+(i+1)+"\t"+query_name[i]+" "+article_name.length) ;
			//System.out.println() ;
			out.print("\r\n") ;
			for(int d=0;d<E_list_D.length;d++){
				//System.out.print(E_list_D[d]+" "+logP_qord[d]+"\r\n") ;
				out.print(E_list_D[d]+" "+logP_qord[d]+"\r\n") ;
			}
			out.print("\r\n") ;
			//System.out.println() ;
		}		
		out.close();
	}



	private static double logP_qord_exe(Hashtable<String, Integer> wf_of_q, Hashtable<String, Integer> Wdf_of_d,
			Hashtable<String, Integer> q_d_has, double[] p_corpus, double[] wofgroup) throws IOException {
		double larmda = 0.059 ; //alpha可調變參數
		double beta = 0.008 ; //K-means beta調變參數
		double sum = 0 ;
		Enumeration<String> qkey = wf_of_q.keys();
		String qword = null ;
		
		while(qkey.hasMoreElements()){
			qword = qkey.nextElement();
			if(q_d_has.containsKey(qword) && !qword.equals("total_length")){
				double a = ((double)(wf_of_q.get(qword)*Math.log(larmda*((double)(q_d_has.get(qword))/
						(double)(Wdf_of_d.get("total_length")))+(1-larmda)*(p_corpus[Integer.parseInt(qword)])
						+ (beta)*(wofgroup[Integer.parseInt(qword)]))));
				sum = sum + a ;
			}
			else{
				if(!qword.equals("total_length")){
					double b = (1-larmda)*(p_corpus[Integer.parseInt(qword)])
							+ (beta)*(wofgroup[Integer.parseInt(qword)]) ;
					double a = Math.log(b) ;
					sum = sum +((double)(wf_of_q.get(qword))*a);
				}
			}
		}
		 
		return sum;
	}
	
	private static Hashtable<String,Hashtable<String,Integer>> open_exe(String[] F_name, String F_path) throws IOException {
		
		Hashtable<String,Hashtable<String,Integer>> q_w = new Hashtable<String,Hashtable<String,Integer>>();
		int total_length = 0 ;
		
		for(int i=0;i<F_name.length;i++){
			total_length = 0 ;//總字數
			//每次讀一個file建一個word frequent表(word frequent / in article length)
			Hashtable<String,Integer> qtab = new Hashtable<String,Integer>();
			Scanner rqfile = new Scanner(new File(F_path+"/"+F_name[i])); //path
			String line = null;
			//open query and execute word frequent in query
			int first_three = 0;
			while(rqfile.hasNext() && F_name[i].contains("VOM") && first_three<3){
				rqfile.nextLine() ;
				first_three++;
			}
			while(rqfile.hasNext()){
				line = rqfile.nextLine() ;
				String[] cutline = line.split(" ");
				//loop newline word
				for(int j=0;j<cutline.length;j++){
					if(!cutline[j].equals("-1")){						
						//if boolean is false to represent the string not exist; otherwise 
						if(!qtab.containsKey(cutline[j])){
							qtab.put(cutline[j],1);
						}
						else{
							int val = qtab.get(cutline[j]);
							val++ ;
							qtab.put(cutline[j], val);
						}
						total_length++ ;
					}
				}
			}
			qtab.put("total_length", total_length);
			q_w.put(F_name[i], qtab);
		}
		return q_w;
		
	}

}
