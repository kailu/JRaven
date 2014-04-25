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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GBMModel {

	private int treeNumber = 0;
	private List<Tree> trees = new ArrayList<Tree>();
	private float initF = 0;
	//default to only one class
	private int classNum = 1;
	private List<String> classNames = new ArrayList<String>();
	
	private String distribution = "";
	
	private List<ArrayList<Integer>> splits = new ArrayList<ArrayList<Integer>>();
	
	private Feature[] allFeatures = null;
	
	class Node{
		public int id;
		public int featureIndex = -1;
		public float splitVal;
		public Node leftNode = null;
		public Node rightNode = null;
		public Node missingNode = null;
		public float prediction;
	}
	
	class Tree{
		public int id;
		public int nodeNo;
		
		Node rootNode = null;
	}
	
	
	
	public Map<String, Float> predict(ArrayList<Object> features) {
		Map<String, Float> retVal = new HashMap<String, Float>();

		Float[] scores = new Float[this.classNames.size()];

		

		for (int i = 0; i < this.classNames.size(); i++)
			scores[i] = this.initF;

		for (int classIndex = 0; classIndex < this.classNum; classIndex++) {
			for (int i = classIndex; i < this.trees.size(); i += this.classNum) {
				// retVal += this.predictWithTree(features, i);
				scores[classIndex] += this.predictWithTree(features, i);
			}
		}

		
		if("multinomial".equalsIgnoreCase(this.distribution)){
			float sum = 0.0f;
			for(int i = 0; i < scores.length; i++){
				scores[i] = (float)Math.exp(scores[i]);
				sum += scores[i];
			}
			for(int i = 0; i < scores.length; i++){
				scores[i] /= sum;
			}
		}else if("bernoulli".equalsIgnoreCase(this.distribution) ||
				"pairwise".equalsIgnoreCase(this.distribution)){
			for(int i = 0; i < scores.length; i++){
				scores[i] = 1.0f/(1 + (float)Math.exp(-1*scores[i]));
			}
		}else if("poisson".equalsIgnoreCase(this.distribution)){
			for(int i = 0; i < scores.length; i++){
				scores[i] = (float)Math.exp(scores[i]);
			}
		}else if("adaboost".equalsIgnoreCase(this.distribution)){
			for(int i = 0; i < scores.length; i++){
				scores[i] = 1.0f/(1 + (float)Math.exp(-2*scores[i]));
			}
		}

		for (int i = 0; i < scores.length; i++) {
			retVal.put(this.classNames.get(i), scores[i]);
		}
		
		return retVal;
	}
	
	public float predictWithTree(ArrayList<Object> features, int ith){
		assert(ith >= 0 && ith < this.treeNumber*this.classNum);
		
		Tree tree = this.trees.get(ith);
		if(tree.rootNode != null){
			Node tmpNode = tree.rootNode;
			while(tmpNode.featureIndex != -1 && tmpNode != null){
				assert tmpNode.featureIndex >= 0 && tmpNode.featureIndex < features.size() && tmpNode.featureIndex < this.allFeatures.length:"Incorrect feature dimension...";
				Object featureVal = features.get(tmpNode.featureIndex);
				if(featureVal == null){
					tmpNode = tmpNode.missingNode;
					continue;
				}
				
				Feature.TYPE type = this.allFeatures[tmpNode.featureIndex].type();
				if(type == Feature.TYPE.CATEGORIES){
					int levelIndex = this.allFeatures[tmpNode.featureIndex].levleIndex((String)featureVal);
					if(levelIndex == -1){
						//suppose not happen, but this category feature might not occurs in training data, treat it as missing node
						tmpNode = tmpNode.missingNode;
					}else{
						int categoryVal = this.splits.get((int)tmpNode.splitVal).get(levelIndex);
						
						switch(categoryVal){
						case -1:
							//go left
							tmpNode = tmpNode.leftNode;
							break;
						case 1:
							//go right
							tmpNode = tmpNode.rightNode;
							break;
						default:
							//treat as missing, suppose never happen
							tmpNode = tmpNode.missingNode;
							break;
						}
					}
				}else{
					Float realVal = (Float)featureVal;
					if(realVal < tmpNode.splitVal){
						//go left
						tmpNode = tmpNode.leftNode;
					}else{
						//go right
						tmpNode = tmpNode.rightNode;
					}
				}
			}
			
			assert tmpNode != null && tmpNode.featureIndex == -1:"Incorrect for predict tree:"+ith;
			//return tmpNode.prediction;
			return tmpNode.splitVal;
		}
		
		return 0.0f;
	}
	
	//load model from xml file
	public GBMModel(String modelPath) throws ParserConfigurationException, SAXException, IOException{
		File xmlFile = new File(modelPath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dbuilder = dbFactory.newDocumentBuilder();
		Document doc = dbuilder.parse(xmlFile);
		
		doc.getDocumentElement().normalize();
		
		this.initF = Float.parseFloat(doc.getElementsByTagName("initF").item(0).getAttributes().getNamedItem("val").getNodeValue());
		this.classNum = Integer.parseInt(doc.getElementsByTagName("classes").item(0).getAttributes().getNamedItem("num").getNodeValue());
		if(this.classNum > 1){
			for(int i = 0; i < this.classNum; i++)
				this.classNames.add(doc.getElementsByTagName("classes").item(0).getAttributes().getNamedItem("class"+(i+1)).getNodeValue()
					);
		}else if(this.classNum == 1){
			this.classNames.add("score");
		}
		
		NodeList nl = doc.getElementsByTagName("trees"); 
		this.treeNumber = Integer.parseInt(nl.item(0).getAttributes().getNamedItem("num").getNodeValue());
		
		nl = doc.getElementsByTagName("tree");
		assert nl.getLength() == this.treeNumber*this.classNum : "Model is incorrect, it supposes to have "+this.treeNumber*this.classNum+"trees, but it only has "+nl.getLength() + " trees.";
		
		this.distribution = doc.getElementsByTagName("distribution").item(0).getAttributes().getNamedItem("name").getNodeValue();
		
		
		//start loading new model
		if(trees == null) trees = new ArrayList<Tree>();
		trees.clear();
		
		for(int i = 0; i < nl.getLength(); i++){
			org.w3c.dom.Element n = (Element)nl.item(i);
			
			
			Tree tree = new Tree();
			trees.add(tree);
			tree.id = Integer.parseInt(n.getAttribute("index"));
			tree.nodeNo = Integer.parseInt(n.getAttribute("nodes"));
			
			NodeList nodes = n.getElementsByTagName("node");  
			
			
			assert (nodes.getLength() == tree.nodeNo): "total sub node lenght:"+nodes.getLength() + " But it supposes to have:"+tree.nodeNo;
			
		
			
			Stack<org.w3c.dom.Node> stack = new Stack<org.w3c.dom.Node>();
			
			//push the first node item
			stack.push(nodes.item(0));
			Map<Integer,Node> allNodes = new HashMap<Integer,Node>();
			
			while(!stack.empty()){
				org.w3c.dom.Node curNode = stack.pop();
				
				NamedNodeMap nnm = curNode.getAttributes();
				
				int nodeIndex = Integer.parseInt(nnm.getNamedItem("index").getNodeValue());
				if(!allNodes.containsKey(nodeIndex)){
					assert nodeIndex == 0:"Only the first node should be created here...";
					allNodes.put(nodeIndex, new Node());
				}
				
				Node node = allNodes.get(nodeIndex);
				node.id = nodeIndex;
				node.featureIndex = Integer.parseInt(nnm.getNamedItem("SplitVar").getNodeValue());
				node.splitVal = Float.parseFloat(nnm.getNamedItem("SplitCodePred").getNodeValue());
				node.prediction = Float.parseFloat(nnm.getNamedItem("Prediction").getNodeValue());
				int leftNode = Integer.parseInt(nnm.getNamedItem("LeftNode").getNodeValue());
				int rightNode = Integer.parseInt(nnm.getNamedItem("RightNode").getNodeValue());
				int missingNode = Integer.parseInt(nnm.getNamedItem("MissingNode").getNodeValue());
				if(node.featureIndex != -1){
					//terminate node
					if(leftNode != -1 && !allNodes.containsKey(leftNode)){
					
						allNodes.put(leftNode, new Node());
						node.leftNode = allNodes.get(leftNode);
						stack.push(nodes.item(leftNode));
					}
					
					if(rightNode != -1 && !allNodes.containsKey(rightNode)){
						allNodes.put(rightNode, new Node());
						node.rightNode = allNodes.get(rightNode);
						stack.push(nodes.item(rightNode));
					}
					
					if(missingNode != -1 && !allNodes.containsKey(missingNode)){
						allNodes.put(missingNode, new Node());
						node.missingNode = allNodes.get(missingNode);
						stack.push(nodes.item(missingNode));
					}
				}
				
			}
			//take the 1st node as the root node of the tree
			if(allNodes.size() > 0)
				tree.rootNode = allNodes.get(0);
		}
		
		//load feature properties
		org.w3c.dom.Element featuresNode = (Element)doc.getElementsByTagName("features").item(0);
		int featureNum = Integer.parseInt(featuresNode.getAttributes().getNamedItem("num").getNodeValue());
		NodeList featureNodeList = featuresNode.getElementsByTagName("feature");
		assert featureNum == featureNodeList.getLength() : "Features setting is incorrectly...";
		this.allFeatures = new Feature[featureNum];
		
		for(int i = 0; i < featureNodeList.getLength(); i++){
			String featureName = featureNodeList.item(i).getAttributes().getNamedItem("name").getNodeValue();
			String isCategory = featureNodeList.item(i).getAttributes().getNamedItem("isCategory").getNodeValue();
			
			if("false".equalsIgnoreCase(isCategory)){
				this.allFeatures[i] = new Feature(featureName,Feature.TYPE.CONTINUES,null);
			}else{
				//get level names
				NodeList levels = ((Element)featureNodeList.item(i)).getElementsByTagName("level");
				String[] levelsName = new String[levels.getLength()];
				for(int levelIndex = 0; levelIndex < levelsName.length; levelIndex++){
					levelsName[levelIndex] = levels.item(levelIndex).getAttributes().getNamedItem("val").getNodeValue();
				}
				this.allFeatures[i] = new Feature(featureName,Feature.TYPE.CATEGORIES,levelsName);
			}
		}
		
		//load splits
		org.w3c.dom.Element splits = (Element)doc.getElementsByTagName("splits").item(0);
		int splitNum = Integer.parseInt(splits.getAttributes().getNamedItem("num").getNodeValue());
		NodeList splitNodeList = splits.getElementsByTagName("split");
		assert splitNum == splitNodeList.getLength() : "Splits number is incorrect...";
		
		for(int splitIndex = 0; splitIndex < splitNodeList.getLength(); splitIndex++){
			NodeList elemNodeList = ((Element)splitNodeList.item(splitIndex)).getElementsByTagName("elem");
			ArrayList<Integer> oneSplit = new ArrayList<Integer>();
			for(int elemIndex = 0; elemIndex < elemNodeList.getLength(); elemIndex++){
				int splitVal = Integer.parseInt(elemNodeList.item(elemIndex).getAttributes().getNamedItem("val").getNodeValue());
				oneSplit.add(splitVal);
			}
			this.splits.add(oneSplit);
		}
	}
}
