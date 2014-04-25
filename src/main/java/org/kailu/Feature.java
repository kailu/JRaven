package org.kailu;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Feature {

	public static enum TYPE{
		CONTINUES,
		CATEGORIES,
	}
	
	//by default, it's an continues feature
	private TYPE type = TYPE.CONTINUES;
	//private List<String> levels = new ArrayList<String>();
	private Map<String,Integer> levels = new HashMap<String,Integer>();
	private String featureName = "";
	
	public TYPE type(){
		return this.type;
	}
	
	public int levleIndex(String levelName){
		if(this.levels.containsKey(levelName)){
			return this.levels.get(levelName);
		}
		
		return -1;
	}
	
	public String name(){
		return this.featureName;
	}
	
	public Feature(String name,Feature.TYPE type,String[] levels){
		this.featureName = name;
		this.type = type;
		if(levels != null){
			for(int i = 0;i < levels.length; i++){
				this.levels.put(levels[i], i);
			}
		}
	}
}
