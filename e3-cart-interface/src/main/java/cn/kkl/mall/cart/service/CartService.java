package cn.kkl.mall.cart.service;


import java.util.List;

import cn.kkl.mall.pojo.E3Result;
import cn.kkl.mall.pojo.TbItem;

public interface CartService {

	E3Result addCart(Long userId,Long itemId, Integer num);
	
	E3Result mergeCart(Long userId,List<TbItem> list);
	
	List<TbItem> getCartList(Long userId);
	
	E3Result updateCartNum(Long userId,Long itemId,Integer num);
	
	E3Result deleteCartItem(Long userId,Long itemId);
	
	E3Result deleteCartItemAll(Long userId);
}
