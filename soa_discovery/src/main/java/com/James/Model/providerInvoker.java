package com.James.Model;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.James.HashFunction.IHashFunction;
import com.James.basic.UtilsTools.CommonConfig;


/**
 * Created by James on 16/6/2.
 */
public class providerInvoker {

  private static final Logger LOGGER = LoggerFactory.getLogger(providerInvoker.class.getName());

  public IHashFunction algo = IHashFunction.MURMUR_HASH;


  //key:method,value:hash环
  private ConcurrentHashMap<String,TreeMap> methodTreeMapNodes= new ConcurrentHashMap();

//  //一致性hash环
//  public TreeMap<Long, SharedProvider> TreeMapNodes = new TreeMap<>();
//
//  public TreeMap<Long, SharedProvider> getTreeMap(){
//    return this.TreeMapNodes;
//  }

  //数字越大,虚拟节点越多,分布越平均
  public static final int basic_virtual_node_number = 40;

  public providerInvoker(){

  }

  //每个method一个hash环
  public providerInvoker init(String method,List<SharedProvider> sharedProviders){
    TreeMap<Long, SharedProvider> TreeMapNodes = new TreeMap<>();
    for(SharedProvider sharedProvider: sharedProviders){
      TreeMapNodes = add(TreeMapNodes,sharedProvider);
    }
    methodTreeMapNodes.put(method,TreeMapNodes);

    return this;
  }



  //在环上获取节点
  public SharedProvider get(String method,String seed) {
    TreeMap TreeMapNodes = methodTreeMapNodes.get(method);
    SortedMap<Long, SharedProvider> tail = TreeMapNodes.tailMap(algo.hash(seed.getBytes(CommonConfig.CHARSET)));
    if (tail.isEmpty()) {
      Map.Entry<Long, SharedProvider> firstEntry = TreeMapNodes.firstEntry();
      if (firstEntry != null) {
        return firstEntry.getValue();
      }
      return null;
    }
    return tail.get(tail.firstKey());
  }

  //计算一致性hash的key后加入环中
  private TreeMap add(TreeMap TreeMapNodes ,SharedProvider sharedProvider) {
    for (int n = 0; n < basic_virtual_node_number ; n++) {
      try {
        Long key = this.algo.hash(
            new StringBuilder(sharedProvider.getIdentityID())
                .append("*")
                .append(n).toString()
            , CommonConfig.CHARSET);

        TreeMapNodes.put(key, sharedProvider);

      } catch (UnsupportedEncodingException e) {
        LOGGER.error("添加节点失败", e);

      }
    }
    return TreeMapNodes;
  }

  //从环中删除
  public TreeMap remove(String method,SharedProvider sharedProvider) {
    TreeMap TreeMapNodes = methodTreeMapNodes.get(method);
    for (int n = 0; n < basic_virtual_node_number ; n++) {
      try {
        Long key = this.algo.hash(
            new StringBuilder(sharedProvider.getIdentityID())
                .append("*")
                .append(n).toString()
            , CommonConfig.CHARSET);

        TreeMapNodes.remove(key);

      } catch (UnsupportedEncodingException e) {
        LOGGER.error("删除节点失败", e);

      }
    }
    return TreeMapNodes;
  }

  //TODO
  //不可用节点

  //TODO
  //定时扫描不可用节点
}
