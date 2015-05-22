package de.hahn.blog.treenavigationmasterdetail.view.beans;

import java.util.Iterator;
import java.util.List;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.share.logging.ADFLogger;
import oracle.adf.view.rich.component.rich.data.RichTree;

import oracle.jbo.Key;
import oracle.jbo.Row;
import oracle.jbo.uicli.binding.JUCtrlHierBinding;
import oracle.jbo.uicli.binding.JUCtrlHierNodeBinding;
import oracle.jbo.uicli.binding.JUCtrlHierTypeBinding;
import oracle.jbo.uicli.binding.JUIteratorBinding;

import org.apache.myfaces.trinidad.event.RowDisclosureEvent;
import org.apache.myfaces.trinidad.event.SelectionEvent;
import org.apache.myfaces.trinidad.model.CollectionModel;
import org.apache.myfaces.trinidad.model.RowKeySet;
import org.apache.myfaces.trinidad.model.TreeModel;


public class TreeNavigationBean {
    private static ADFLogger logger = ADFLogger.createADFLogger(TreeNavigationBean.class);

    public TreeNavigationBean() {
    }

    /**
     * Custom managed bean method that takes a SelectEvent input argument
     * to generically set the current row corresponding to the selected row
     * in the tree. Note that this method is a way to replace "makeCurrent"
     * EL expression (#{bindings.<tree binding>. treeModel.makeCurrent}that
     * Oracle JDeveloper adds to the tree component SelectionListener
     * property when dragging a collection from the Data Controls panel.
     * Using this custom selection listener allows developers to add pre-
     * and post processing instructions. For example, you may enforce PPR
     * on a specific item after a new tree node has been selected. This
     * methods performs the following steps
     *
     * i. get access to the tree component
     * ii. get access to the ADF tree binding
     * iii. set the current row on the ADF binding
     * iv. get the information about target iterators to synchronize
     * v. synchronize target iterator
     *
     * @param selectionEvent object passed in by ADF Faces when configuring
     * this method to become the selection listener
     *
     * @author Frank Nimphius
     */
    public void onTreeSelection(SelectionEvent selectionEvent) {

        /* custom pre processing goes here */
        /* --- */

        //get the tree information from the event object
        RichTree tree1 = (RichTree)selectionEvent.getSource();
        //in a single selection case ( a setting on the tree component ) the
        //added set only has a single entry. If there are more then using this
        //method may not be desirable. Implicitly we turn the multi select in a
        //single select later, ignoring all set entries than the first
        RowKeySet rks2 = selectionEvent.getAddedSet();
        //iterate over the contained keys. Though for a single selection use
        //case we only expect one entry in here
        Iterator<Object> rksIterator = (Iterator<Object>)rks2.iterator();

        //support single row selection case
        if (rksIterator.hasNext()) {
            //get the tree node key, which is a List of path entries describing
            //the location of the node in the tree including its parents nodes
            List<Object> key = (List<Object>)rksIterator.next();
            //get the ADF tree binding to work with
            JUCtrlHierBinding treeBinding = null;
            //The Trinidad CollectionModel is used to provide data to trees and
            //tables. In the ADF binding case, it contains the tree binding as
            //wrapped data
            treeBinding = (JUCtrlHierBinding)((CollectionModel)tree1.getValue()).getWrappedData();
            //find the node identified by the node path from the ADF binding
            //layer. Note that we don't need to know about the name of the tree
            //binding in the PageDef file because
            //all information is provided
            JUCtrlHierNodeBinding nodeBinding = nodeBinding = treeBinding.findNodeByKeyPath(key);
            if (nodeBinding == null) {
                logger.info("nodeBinding is null!");

            }
            //the current row is set on the iterator binding. Because all
            //bindings have an internal reference to their iterator usage,
            //the iterator can be queried from the ADF binding object
            DCIteratorBinding _treeIteratorBinding = null;
            _treeIteratorBinding = treeBinding.getDCIteratorBinding();
            JUIteratorBinding iterator = nodeBinding.getIteratorBinding();
            String keyStr = nodeBinding.getRowKey().toStringFormat(true);
            iterator.setCurrentRowWithKey(keyStr);
            //get selected node type information
            JUCtrlHierTypeBinding typeBinding = nodeBinding.getHierTypeBinding();

            //The tree node rule may have a target iterator defined. Target
            //iterators are configured using the Target Data Source entry in
            //the tree node edit dialog
            //and allow developers to declaratively synchronize an independent
            //iterator binding with the node selection in the tree.
            //
            String targetIteratorSpelString = typeBinding.getTargetIterator();

            //chances are that the target iterator option is not configured. We
            //avoid NPE by checking this condition

            if (targetIteratorSpelString != null && !targetIteratorSpelString.isEmpty()) {

                //resolve SPEL string for target iterator
                DCIteratorBinding targetIterator = resolveTargetIterWithSpel(targetIteratorSpelString);
                //synchronize the row in the target iterator
                Key rowKey = nodeBinding.getCurrentRow().getKey();
                targetIterator.setCurrentRowWithKey(rowKey.toStringFormat(true));
            }

            /* custom post processing goes here */
            /* --- */

        }
    }

    /**
     * Custom managed bean method that takes a SelectEvent input argument
     * to generically set the current row corresponding to the selected row
     * in the tree.
     *
     * This method is not generic as it uses the normal binding.iterator.model.makecurrent the ui component uses.
     * The child iterator must be known too to select the child not in the chile view.
     *
     * @param selectionEvent object passed in by ADF Faces when configuring
     * this method to become the selection listener
     */
    public void onTreeNodeSelection(SelectionEvent selectionEvent) {
        //original selection listener set by ADF
        //#{bindings.Departments.treeModel.makeCurrent}
        String adfSelectionListener = "#{bindings.Departments.treeModel.makeCurrent}";
        //make sure the default selection listener functionality is
        //preserved. you don't need to do this for multi select trees
        //as the ADF binding only supports single current row selection

        /* START PRESERVER DEFAULT ADF SELECT BEHAVIOR */
        FacesContext fctx = FacesContext.getCurrentInstance();
        Application application = fctx.getApplication();
        ELContext elCtx = fctx.getELContext();
        ExpressionFactory exprFactory = application.getExpressionFactory();

        MethodExpression me = null;
        me = exprFactory.createMethodExpression(elCtx, adfSelectionListener, Object.class, new Class[] { SelectionEvent.class });
        me.invoke(elCtx, new Object[] { selectionEvent });

        /* END PRESERVER DEFAULT ADF SELECT BEHAVIOR */
        RichTree tree = (RichTree)selectionEvent.getSource();
        TreeModel model = (TreeModel)tree.getValue();

        //get selected nodes
        RowKeySet rowKeySet = selectionEvent.getAddedSet();
        Iterator<Object> rksIterator = (Iterator<Object>)rowKeySet.iterator();
        //for single select configurations,this only is called once
        while (rksIterator.hasNext()) {
            List<Object> key = (List<Object>)rksIterator.next();
            JUCtrlHierBinding treeBinding = null;
            CollectionModel collectionModel = (CollectionModel)tree.getValue();
            treeBinding = (JUCtrlHierBinding)collectionModel.getWrappedData();
            JUCtrlHierNodeBinding nodeBinding = null;
            nodeBinding = treeBinding.findNodeByKeyPath(key);
            Row rw = nodeBinding.getRow();
            //print first row attribute. Note that in a tree you have to
            //determine the node type if you want to select node attributes
            //by name and not index
            String rowType = rw.getStructureDef().getDefName();

            // check the node type as we don'T have to do anything is the parent node is selected
            if (rowType.equalsIgnoreCase("DepartmentsView")) {
                logger.info("This row is a department: " + rw.getAttribute("DepartmentId"));
            } else if (rowType.equalsIgnoreCase("EmployeesView")) {
                // for the child node we search the row which was selected in the tree
                logger.info("This row is an employee: " + rw.getAttribute("EmployeeId"));
                Object attribute = rw.getAttribute("EmployeeId");
                // make the selected row the current row in the child iterator
                DCBindingContainer dcBindings = (DCBindingContainer)BindingContext.getCurrent().getCurrentBindingsEntry();
                DCIteratorBinding iterBind = (DCIteratorBinding)dcBindings.get("EmployeesOfDepartmentsIterator");
                iterBind.setCurrentRowWithKeyValue(attribute.toString());
            } else {
                // tif you end here your tree has more then two node types
                logger.info("Huh????");
            }
            // ... do more useful stuff here
        }
    }

    /**
     * Helper method to resolve EL expression into DCIteratorBinding
     * instance
     * @param spelExpr the SPEL expression starting with ${...}
     * @return DCIteratorBinding instance
     */
    private DCIteratorBinding resolveTargetIterWithSpel(String spelExpr) {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ELContext elctx = fctx.getELContext();
        ExpressionFactory elFactory = fctx.getApplication().getExpressionFactory();
        ValueExpression valueExpr = elFactory.createValueExpression(elctx, spelExpr, Object.class);
        DCIteratorBinding dciter = (DCIteratorBinding)valueExpr.getValue(elctx);
        return dciter;
    }

    public void onRowDisclosure(RowDisclosureEvent rowDisclosureEvent) {
        RowKeySet keyRemoved = rowDisclosureEvent.getRemovedSet();
        RowKeySet keyAdded = rowDisclosureEvent.getAddedSet();
        logger.info("removed: " + keyRemoved.size() + " added: " + keyAdded.size());
        SelectionEvent se = new SelectionEvent(keyRemoved, keyAdded, rowDisclosureEvent.getComponent());
        logger.info("Call tree selection event!");
        onTreeSelection(se);
    }
}
