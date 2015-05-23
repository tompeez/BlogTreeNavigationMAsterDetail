package de.hahn.blog.treenavigationmasterdetail.view.beans;

import java.util.Iterator;
import java.util.List;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.share.logging.ADFLogger;
import oracle.adf.view.rich.component.rich.data.RichTree;

import oracle.jbo.Row;
import oracle.jbo.uicli.binding.JUCtrlHierBinding;
import oracle.jbo.uicli.binding.JUCtrlHierNodeBinding;

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
}
