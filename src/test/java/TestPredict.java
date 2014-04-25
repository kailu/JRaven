

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


import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.kailu.GBMModel;
import org.xml.sax.SAXException;


public class TestPredict {
	
	@Test
	public void testModel1() throws ParserConfigurationException, SAXException, IOException{
		String baseDir = System.getProperty("basedir");
		
		GBMModel model = new GBMModel(baseDir + "/src/test/resources/model.xml");
				
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(baseDir + "/src/test/resources/predict.csv"));
		while((line = br.readLine()) != null){
			String[] elements = line.split("\t");
			ArrayList<Object> feature = new ArrayList<Object>();
			
			for(int i = 1; i < elements.length; i++){
				feature.add(Float.parseFloat(elements[i]));
			}
			
			System.out.println(model.predict(feature));
		}
	}
}
