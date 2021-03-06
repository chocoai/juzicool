/**
 * 请勿将俱乐部专享资源复制给其他人，保护知识产权即是保护我们所在的行业，进而保护我们自己的利益
 * 即便是公司的同事，也请尊重 JFinal 作者的努力与付出，不要复制给同事
 * 
 * 如果你尚未加入俱乐部，请立即删除该项目，或者现在加入俱乐部：http://jfinal.com/club
 * 
 * 俱乐部将提供 jfinal-club 项目文档与设计资源、专用 QQ 群，以及作者在俱乐部定期的分享与答疑，
 * 价值远比仅仅拥有 jfinal club 项目源代码要大得多
 * 
 * JFinal 俱乐部是五年以来首次寻求外部资源的尝试，以便于有资源创建更加
 * 高品质的产品与服务，为大家带来更大的价值，所以请大家多多支持，不要将
 * 首次的尝试扼杀在了摇篮之中
 */

package com.juzicool.search.plugin;

import com.jfinal.plugin.activerecord.Page;
import com.juzicool.search.Juzi;
import com.juzicool.search.JuziObject;
import com.juzicool.search.util.JuziUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;

/**
 * 首页业务，主要为了方便做缓存，以及排序逻辑
 */
public class SearchService {
    private static Logger log = Logger.getLogger(ElasticSearch.class);
	private static final String INDEXS = "juzicool";
	private static final String INDEX_NAME_TYPE = "juzi";
	
	public static final SearchService me = new SearchService();

	ElasticSearch es =  ElasticSearch.get();
	
	/**
	 * 更新句子的索引
	 */
	public void updateSearchIndex(List<Juzi> juziList) {
			/*CountDownLatch latch = new CountDownLatch(juziList.size());
			RestClient client = es.requestClient();
			try {
			
				for(Juzi juzi: juziList) {
					String jsonString = JuziUtils.toJson(juzi);
					
					//juziId++;
					Request request = new Request("put",  "/"+INDEXS+"/"+INDEX_NAME_TYPE+"/"+juzi);
	
					request.setJsonEntity(jsonString);
					client.performRequestAsync(request, new ResponseListener() {
						
						@Override
						public void onSuccess(Response arg0) {
							latch.countDown();
						}
						
						@Override
						public void onFailure(Exception arg0) {
							latch.countDown();
					
						}
					});
				}
				latch.await();
			}catch(Exception ex) {
				log.warn(ex.getMessage(), ex);
			}finally {
				es.releaseClient(client);
			}
			*/
		
	}
	
	/**
	 * 
	 * @param query
	 * @param pageSize
	 * @return
	 */
	public Page<JuziObject> query(String query,final int currenPage,final int pageSize){
		
		if(currenPage < 1 || pageSize < 1) {
			throw new RuntimeException("error index must > 0 ");
		}
		int pageIndex = currenPage - 1;
		int offset = pageIndex * pageIndex;

		RestClient client = es.requestClient();
		
		String jsonResult = null;
		
		try {
			jsonResult = query(client,query,offset,pageSize);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			return null;
		}finally {
			//	第一时间释放链接实例
			es.releaseClient(client);
		}

		
		try {
		
			JSONObject json = JSONObject.parseObject(jsonResult);
		
			if(json.containsKey("hits")) {
				JSONObject hitsObject = json.getJSONObject("hits");
				ArrayList<JuziObject> juziList = new ArrayList<>();
				
				int total = hitsObject.getInteger("total");
				JSONArray hitsArray = hitsObject.getJSONArray("hits");
				int length = hitsArray!= null ? hitsArray.size():0;
				for(int i = 0; i < length;i++) {
					JSONObject juziItem = hitsArray.getJSONObject(i).getJSONObject("_source");
					
					
					String applyDesc = juziItem.getString("applyDesc");
					String author = juziItem.getString("author");
					String category = juziItem.getString("category");
					String content = toHtml(juziItem.getString("content"));
					String from = juziItem.getString("from");
					//juzi.length = juziItem.getIntValue("length");
					String remark = juziItem.getString("remark");
					String tags = juziItem.getString("tags");
					long  updateTime = juziItem.getLongValue("updateTime");
					
					Juzi juzi = new Juzi();
					
					juzi.setApplyDesc(applyDesc);
					juzi.setAuthor(author);
					juzi.setContent(content);
					juzi.setCategory(category);
					juzi.setFrom(from);
					juzi.setRemark(remark);
					juzi.setTags(tags);
					juzi.setLength(content.length());
					juzi.setUpdateAt(new Date(updateTime));
					
					juziList.add(juzi);
				}
				
				int totalPage = total / pageSize;
				if(total%pageSize != 0) {
					totalPage++;
				}
				
				return new Page<JuziObject>(juziList,pageIndex,pageSize,totalPage,total);
			}
			
			//json.get

		}catch(Exception ex){
			log.warn(ex.getMessage(), ex);
		}
		
		return null;
	} 
	
	public static String query(RestClient restClient,String keyword,int offset,int size)throws Exception {
		
		Request r = new Request("GET", "/juzicool/juzi/_search");
		String json = "{" + 
				"     \"query\": {" + 
				"        \"multi_match\": {" + 
				"            \"query\":  \""+keyword+"\"," + 
				"            \"type\":   \"most_fields\", " + 
				"            \"fields\": [ \"tags\",\"remark\",\"applyDesc\",\"content\",\"category\",\"author\" ,\"from\"]," + 
				"            \"analyzer\": \"ik_smart\"" + 
				"          " + 
				"        }" + 
				"    }," + 
				"    \"size\":"+size+"," + 
				"    \"from\":"+offset+"" + 
				"}";
		r.setJsonEntity(json);
				
		Response reponse = restClient.performRequest(r);

		HttpEntity entity = reponse.getEntity();
		
		return EntityUtils.toString(entity, "utf8");
	
	}
	
	private static String toHtml(String text) {
		if(text == null) {
			return null;
		}
		
		text = text.replaceAll("\r\n", "<br/>");
		text = text.replaceAll("\r", "<br/>");
		text = text.replaceAll("\n", "<br/>");

		return text;
	}
}



