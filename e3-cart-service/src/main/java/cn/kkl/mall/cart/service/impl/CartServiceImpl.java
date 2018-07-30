package cn.kkl.mall.cart.service.impl;


import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.kkl.mall.cart.service.CartService;
import cn.kkl.mall.mapper.TbItemMapper;
import cn.kkl.mall.pojo.E3Result;
import cn.kkl.mall.pojo.TbItem;
import cn.kkl.mall.service.JedisClient;
import cn.kkl.mall.utils.JsonUtils;

@Service
public class CartServiceImpl implements CartService {
	
	@Value("${REDIS_CART_PRE}")
	private String reidsCartPre;
	
	@Autowired
	private JedisClient jedisClient;
	
	@Autowired
	private TbItemMapper itemMapper;

	/*add cart list to redis logic:
	 * 1. use data type hash in redis,userId as key,itemId as field,value is itemInformation
	 * 2. judge the item does it exists:
	 * 	  	if exists , item number add up
	 * 		if not exists, invoke manager service query item information by itemId,then add this item
	 * 3. add item in redis
	 * 4. return success 
	 */
	@Override
	public E3Result addCart(Long userId, Long itemId,Integer num) {
		Boolean hexists = jedisClient.hexists(reidsCartPre+":"+String.valueOf(userId), String.valueOf(itemId));
		if (hexists) {
			String hgetJson = jedisClient.hget(reidsCartPre+":"+String.valueOf(userId), String.valueOf(itemId));
			TbItem item = JsonUtils.jsonToPojo(hgetJson, TbItem.class);
			item.setNum(item.getNum()+num);
			jedisClient.hset(reidsCartPre+":"+String.valueOf(userId), String.valueOf(itemId), JsonUtils.objectToJson(item));
			return E3Result.ok();
		}
		TbItem tbItem = itemMapper.selectByPrimaryKey(itemId);
		tbItem.setNum(num);
		tbItem.setImage(StringUtils.isBlank(tbItem.getImage().split(",")[0])?"":tbItem.getImage().split(",")[0]);
		jedisClient.hset(reidsCartPre+":"+String.valueOf(userId), String.valueOf(itemId), JsonUtils.objectToJson(tbItem));
		return E3Result.ok();
	}

	/* merger cart logic:
	 * 1. polling cartList ,before adding every item in cartList to current userId as hash key,itemId as field to redis
	 *   need to judge redis cart if exists the item,if exists add up the quantity,if not add item  
	 */
	@Override
	public E3Result mergeCart(Long userId, List<TbItem> list) {
		for (TbItem tbItem : list) {
			addCart(userId, tbItem.getId(), tbItem.getNum());	
		}
		return E3Result.ok();
	}

	/*get cartList from redis logic:
	 * get cartList from redis dependent userId 
	 *
	 */
	@Override
	public List<TbItem> getCartList(Long userId) {
		List<TbItem> cartList=new ArrayList<>();
		List<String> hvals = jedisClient.hvals(reidsCartPre+":"+String.valueOf(userId));
		if (hvals==null || hvals.size()==0) {
			 System.out.println("get cartList from reids is null ,please check");
			 return cartList;
		}
		for (String string : hvals) {
			TbItem item = JsonUtils.jsonToPojo(string, TbItem.class);
			cartList.add(item);
		}
		return cartList;
	}

	/* update redis cart list number logic:
	 * 1. get item information from redis dependent on userId and itemId
	 * 2. update the item number,write the item information to redis again
	 * 3. return update success
	 */
	@Override
	public E3Result updateCartNum(Long userId, Long itemId, Integer num) {
		String hget = jedisClient.hget(reidsCartPre+":"+String.valueOf(userId), String.valueOf(itemId));
		if (StringUtils.isBlank(hget)) {
			return E3Result.build(400, "get cart item informaiton from redis is null,please check");
		}
		TbItem item = JsonUtils.jsonToPojo(hget, TbItem.class);
		item.setNum(num);
		jedisClient.hset(reidsCartPre+":"+String.valueOf(userId), String.valueOf(itemId), JsonUtils.objectToJson(item));
		return E3Result.ok();
	}

	/* delete redis cart list specific itemId itemInformaiton logic:
	 * delete the item information dependent userId and itemId directly
	 * return delete success
	 */
	@Override
	public E3Result deleteCartItem(Long userId, Long itemId) {
		jedisClient.hdel(reidsCartPre+":"+String.valueOf(userId), String.valueOf(itemId));
		return E3Result.ok();
	}
	
	
	
	

}
