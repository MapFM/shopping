package com.lyu.shopping.front.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.github.pagehelper.PageInfo;
import com.lyu.shopping.common.dto.PageParam;
import com.lyu.shopping.recommendate.dto.UserActiveDTO;
import com.lyu.shopping.recommendate.dto.UserSimilarityDTO;
import com.lyu.shopping.recommendate.service.UserActiveService;
import com.lyu.shopping.recommendate.service.UserSimilarityService;
import com.lyu.shopping.recommendate.util.RecommendUtils;
import com.lyu.shopping.sysmanage.dto.Category1DTO;
import com.lyu.shopping.sysmanage.dto.ProductDTO;
import com.lyu.shopping.sysmanage.entity.Category1;
import com.lyu.shopping.sysmanage.entity.Product;
import com.lyu.shopping.sysmanage.service.Category1Service;
import com.lyu.shopping.sysmanage.service.Category2Service;
import com.lyu.shopping.sysmanage.service.ProductService;

/**
 * 类描述：用于处理前台商城页面的请求<br/>
 * 类名称：com.lyu.shopping.front.controller.IndexController
 * @author 曲健磊
 * 2018年3月21日.下午4:11:35
 * @version V1.0
 */
@Controller
public class IndexController {

	/**
	 * 负责打印日志
	 */
	private Logger logger = Logger.getLogger(IndexController.class);
	
	@Autowired
	private Category1Service category1Service;
	
	@Autowired
	private Category2Service category2Service;

	@Autowired
	private ProductService productService;
	
	@Autowired
	private UserSimilarityService userSimilarityService;

	@Autowired
	private UserActiveService userActiveService;
	
	/**
	 * 充当当前登录的用户，当商城界面用户的功能完成后再做成动态的
	 */
	private static final Long currUId = 1L;

	private volatile ServletContext servletContext;
	
	/**
	 * 处理前往商城首页的请求
	 * @return 商城首页的视图名称
	 */
	@RequestMapping("/index")
	public String gotoIndex(HttpSession session) {
		// 在前往商城首页的时候要加载出所有的一级类目以及每个一级类目下的二级类目
//		Category1 category1 = new Category1();
//		category1.setShowFlag(1);
//		List<Category1DTO> category1DTOList = this.category1Service.listCategory1DTO(category1);
//		session.setAttribute("category1List", category1DTOList);

		// 通过基于用户的协同过滤的推荐算法计算出需要给用户推荐出的商品
		// 1.获取当前登录的用户(先暂时把当前的用户id定为1L，待后续功能完善后再做补充)
//		Long currU Id = 1L;

		// 2.找到当前用户与其他用户的相似度列表
		List<UserSimilarityDTO> userSimilarityList = this.userSimilarityService.listUserSimilarityByUId(currUId);
		
		// 3.找到与当前用户相似度最高的topN个用户
		Integer topN = 5;
		List<Long> userIds = RecommendUtils.getSimilarityBetweenUsers(currUId, userSimilarityList, topN);
		
		// 4.从这N个用户中先找到应该推荐给用户的二级类目的id
		List<UserActiveDTO> userActiveList = userActiveService.listAllUserActive();
		List<Long> category2List = RecommendUtils.getRecommendateCategory2(currUId, userIds, userActiveList);
		
		// 5.找到这些二级类目下点击量最大的商品
		List<Product> recommendateProducts = new ArrayList<Product>();
    	for (Long category2Id : category2List) {
    		List<ProductDTO> productList = productService.listProductByCategory2Id(category2Id);
    		// 找出当前二级类目中点击量最大的商品
    		Product maxHitsProduct = RecommendUtils.findMaxHitsProduct(productList);
    		recommendateProducts.add(maxHitsProduct);
    	}
    	
    	// 把待推荐的商品放入session中，便于前台展示
    	session.setAttribute("recommendateList", recommendateProducts);
		return "front/main/main";
	}

	/**
	 * 根据一级类目查找该一级类目下的所有商品,以及通过一对多关联查询所有的二级类目
	 * @param session 本次会话域
	 * @param request 本次请求域
	 * @param category1Id 要查询的一级类目的id
	 * @param pageNum 当前要查询的页数
	 * @param pageSize 每页的大小
	 * @return 显示指定一级类目下的商品列表的视图名称
	 */
	@RequestMapping("/findProductListByCategory1Id/{category1Id}/{pageNum}/{pageSize}")
	public String findProductListByCategory1Id(HttpSession session, HttpServletRequest request,
		@PathVariable("category1Id") Long category1Id, @PathVariable("pageNum") Integer pageNum,
		@PathVariable("pageSize") Integer pageSize) {

		/*servletContext = session.getServletContext();
		@SuppressWarnings("unchecked")
		List<Category1DTO> category1DTOList = (List<Category1DTO>) servletContext.getAttribute("category1List");
		if (category1DTOList == null) {
			Category1 category1 = new Category1();
			category1.setShowFlag(1);
			category1DTOList = this.category1Service.listCategory1DTO(category1);
			servletContext.setAttribute("category1List", category1DTOList);
		}*/

		// 2.查询出该一级类目下的所有商品
		Product product = new Product();
		product.setCategory1Id(category1Id);
		PageParam pageParam = new PageParam(pageNum, pageSize);
		PageInfo<ProductDTO> productList = this.productService.listProductPage(product, pageParam);
		session.setAttribute("productList", productList.getList());

		// 3.放置分页条的相关信息
		setPageAttribute(request, category1Id, productList, "findProductListByCategory1Id");
		
		// 4.将该一级类目下的所有二级类目的点击量+1
		// 4.1查询出当前一级类目下的所有二级类目
		/*Category2 category2 = new Category2();
		category2.setCategory1Id(category1Id);
		List<Category2DTO> category2List = this.category2Service.listCategory2(category2);
		// 4.2记录当前用户对这些二级类目的浏览次数
		for (Category2DTO category2DTO : category2List) {
			UserActiveDTO userActiveDTO = new UserActiveDTO();
			userActiveDTO.setUserId(currUId);
			userActiveDTO.setCategory2Id(category2DTO.getCategory2Id());
			boolean flag = this.userActiveService.saveUserActive(userActiveDTO);
			if (flag) {
				// 打印日志统计用户浏览信息是否成功入库，便于后台进行日志分析
				logger.info("添加一条浏览记录如下：用户id-" + currUId + "，二级类目Id：" + product.getCategory2Id());
			} else {
				logger.info("更新Id为" + currUId + "的用户的一条浏览记录");
			}
		}*/
		
		return "front/productList";
	}

	/**
	 * 根据二级类目查找该二级类目下的所有商品
	 * @param session 本次会话域
	 * @param request 本次请求域
	 * @param category2Id 要查询的二级类目的id
	 * @param pageNum 当前要查询的页数
	 * @param pageSize 每页的大小
	 * @return 显示指定二级类目下的商品列表的视图名称
	 */
	@RequestMapping("/findProductListByCategory2Id/{category2Id}/{pageNum}/{pageSize}")
	public String findProductListByCategory2Id(HttpSession session, HttpServletRequest request,
		@PathVariable("category2Id") Long category2Id, @PathVariable("pageNum") Integer pageNum,
		@PathVariable("pageSize") Integer pageSize) {

		// 1.查询出所有的一级类目以及一级类目下的二级类目列表，用Category1DTO来接收
		// 如果缓存为空说明还没有加载过一级类目列表，或者一级类目列表被修改过了，此时需要重新加载一级类目列表
		@SuppressWarnings("unchecked")
		List<Category1DTO> category1DTOList = (List<Category1DTO>) session.getAttribute("category1List");
		if (category1DTOList == null) {
			Category1 category1 = new Category1();
			category1.setShowFlag(1);
			category1DTOList = this.category1Service.listCategory1DTO(category1);
			session.setAttribute("category1List", category1DTOList);
		}

		// 2.查询出该二级类目下的所有商品
		Product product = new Product();
		product.setCategory2Id(category2Id);
		PageParam pageParam = new PageParam(pageNum, pageSize);
		PageInfo<ProductDTO> productList = this.productService.listProductPage(product, pageParam);
		session.setAttribute("productList", productList.getList());

		// 3.放置分页条的相关信息
		setPageAttribute(request, category2Id, productList, "findProductListByCategory2Id");
		
		// 4.向数据库中添加当前用户对当前二级类目的浏览记录
		UserActiveDTO userActiveDTO = new UserActiveDTO();
		userActiveDTO.setUserId(currUId);
		userActiveDTO.setCategory2Id(category2Id);
		boolean flag = this.userActiveService.saveUserActive(userActiveDTO);
		if (flag) {
			// 打印日志统计用户浏览信息是否成功入库，便于后台进行日志分析
			logger.info("添加一条浏览记录如下：用户id-" + currUId + "，二级类目Id：" + product.getCategory2Id());
		} else {
			logger.info("更新Id为" + currUId + "的用户的一条浏览记录");
		}
		
		return "front/productList";
	}

	/**
	 * 处理前往商品详情页面的请求
	 * @param session 本次会话域
	 * @param request 本次请求域
	 * @param productId 商品的id
	 * @return 商品详情页面的视图名称
	 */
	@RequestMapping("/getProductDetail/{productId}")
	public String getProductDetail(HttpSession session, HttpServletRequest request,
		@PathVariable("productId") Long productId) {
		if (productId == null) {
			return "front/product";
		}

		// 1.查询出所有的一级类目以及一级类目下的二级类目列表，用Category1DTO来接收
		// 如果缓存为空说明还没有加载过一级类目列表，或者一级类目列表被修改过了，此时需要重新加载一级类目列表
		@SuppressWarnings("unchecked")
		List<Category1DTO> category1DTOList = (List<Category1DTO>) session.getAttribute("category1List");
		if (category1DTOList == null) {
			Category1 category1 = new Category1();
			category1.setShowFlag(1);
			category1DTOList = this.category1Service.listCategory1DTO(category1);
			session.setAttribute("category1List", category1DTOList);
		}
		Product product = this.productService.getProductByProductId(productId);
		System.out.println("当前商品的二级类目id：" + product.getCategory2Id());
		
		// 2.记录当前用户对该商品所处二级类目的浏览行为
		UserActiveDTO userActiveDTO = new UserActiveDTO();
		userActiveDTO.setUserId(currUId);
		userActiveDTO.setCategory2Id(product.getCategory2Id());
		boolean flag = this.userActiveService.saveUserActive(userActiveDTO);
		if (flag) {
			// 2.1打印日志统计用户浏览信息是否成功入库，便于后台进行日志分析
			logger.info("添加一条浏览记录如下：用户id-" + currUId + "，二级类目Id：" + product.getCategory2Id());
		} else {
			logger.info("更新Id为" + currUId + "的用户的一条浏览记录");
		}
		
		// 3.增加当前商品的点击量
		boolean hitsFlag = this.productService.updateProductHitsByProductId(productId);
		if (hitsFlag) {
			// 3.1打印日志记录此次对商品点击量的增加
			logger.info("productId为：" + productId + "的商品的点击量+1成功！");
		} else {
			logger.info("productId为：" + productId + "的商品的点击量+1失败！");
		}
		
		request.setAttribute("product", product);
		return "front/product";
	}

	/**
	 * 设置前台分页的属性，例如上一页是第几页，下一页是第几页，首页是多少，尾页是多少，
	 * 当前是一级类目的分页还是二级类目的分页，当前是第几页，总共有多少页...
	 * @param request 当前的请求域
	 * @param categoryId 当前要进行分页的类目的id
	 * @param productList 要进行分页的商品列表
	 * @param pageType 分页的类型：对一级类目进行分页还是二级类目进行分页
	 */
	@SuppressWarnings("deprecation")
	public void setPageAttribute(HttpServletRequest request, Long categoryId, PageInfo<ProductDTO> productList, String pageType) {
		request.setAttribute("pageType", pageType); // 当前是一级类目的分页还是二级类目的分页
		request.setAttribute("currPage", productList.getPageNum()); // 当前是第几页
		request.setAttribute("totalPage", productList.getPages()); // 总共有多少页
		request.setAttribute("firstPage", productList.getFirstPage()); // 首页是第几页
		request.setAttribute("lastPage", productList.getLastPage()); // 尾页是第几页
		request.setAttribute("prePage", productList.getPrePage()); // 上一页是第几页
		request.setAttribute("nextPage", productList.getNextPage()); // 下一页是第几页
		request.setAttribute("categoryId", categoryId); // 当前类目的id
	}

}
