library("gbm");
library("XML");

save.gbm.model <- function(obj,filename){
    tree.no <- obj$n.trees;

    root <- newXMLNode("model");
    initF <- newXMLNode("initF",attrs=c(val=obj$initF),parent=root);
    trees <- newXMLNode("trees",attrs=c(num=tree.no),parent=root);
    classes <- newXMLNode("classes", attrs=c(num=obj$num.classes,class=obj$classes),parent=root);
    distribution <- newXMLNode("distribution",attrs=(val=obj$distribution),parent=root);

    ##save information of category features
    features <- newXMLNode("features",attrs=c(num=length(obj$var.names)),parent=root);
    for( i in 1:length(obj$var.names)){
        feature <- newXMLNode("feature",attrs=c(isCategory=(obj$var.type[[i]] != 0),name=obj$var.names[[i]]),parent=features);
        if(obj$var.type[[i]] != 0){
    #save levels
            for(levelIndex in 0:(length(obj$var.levels[[i]])-1) ){
                level <- newXMLNode("level",attrs=c(val=obj$var.levels[[i]][[levelIndex+1]], index=levelIndex),parent=feature);
            }
        }
    }

    ##save splits
    splits <- newXMLNode("splits",attrs=c(num=length(obj$c.splits)),parent=root);
    if(length(obj$c.splits) > 0){
        for( i in 1:length(obj$c.splits) ){
            split <- newXMLNode("split",attrs=c(num=length(obj$c.splits[[i]]),index=i),parent=splits);
            for(splitIndex in 0:(length(obj$c.splits[[i]])-1) ){
                vv <- newXMLNode("elem",attrs=c(index=splitIndex,val=obj$c.splits[[i]][[splitIndex+1]]),parent=split);
            }
        }
    }

    for( ti in 1:(tree.no*obj$num.classes) ){
        tree <- data.frame(obj$trees[[ti]]);

        names(tree) <- c("SplitVar","SplitCodePred","LeftNode","RightNode","MissingNode","ErrorReduction","Weight","Prediction");
        node.no <- nrow(tree);
        treeNode <- newXMLNode("tree",attrs=c(nodes=node.no,index=ti),parent=trees);
        #start to serialize each tree
        for( ni in 0:(node.no -1) ){
            node <- newXMLNode("node",attrs=c(index=ni,
                        SplitVar = tree[ni+1,"SplitVar"],
                        SplitCodePred = tree[ni+1,"SplitCodePred"],
                        LeftNode = tree[ni+1,"LeftNode"],
                        RightNode = tree[ni+1,"RightNode"],
                        MissingNode = tree[ni+1,"MissingNode"],
                        ErrorReduction = tree[ni+1,"ErrorReduction"],
                        Weight = tree[ni+1,"Weight"],
                        Prediction = tree[ni+1,"Prediction"]
                        ),parent=treeNode);

        }
    }

    saveXML(root,file=filename);

}
