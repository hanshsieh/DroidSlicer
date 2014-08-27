package org.droidslicer;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.droidslicer.graph.BehaviorGraph;
import org.droidslicer.graph.entity.ActivityUnit;
import org.droidslicer.graph.entity.AppComponentUnit;
import org.droidslicer.graph.entity.ComponentReachRelation;
import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.Entity;
import org.droidslicer.graph.entity.EntityVisitor;
import org.droidslicer.graph.entity.FileInputUnit;
import org.droidslicer.graph.entity.FileOutputUnit;
import org.droidslicer.graph.entity.ICCParamCalleeUnit;
import org.droidslicer.graph.entity.ICCParamCallerUnit;
import org.droidslicer.graph.entity.ICCReturnCalleeUnit;
import org.droidslicer.graph.entity.ICCReturnCallerUnit;
import org.droidslicer.graph.entity.ICCUnit;
import org.droidslicer.graph.entity.IInstructionUnit;
import org.droidslicer.graph.entity.IntentCommUnit;
import org.droidslicer.graph.entity.InvocationUnit;
import org.droidslicer.graph.entity.ProviderUnit;
import org.droidslicer.graph.entity.ReceiverUnit;
import org.droidslicer.graph.entity.RelationEntity;
import org.droidslicer.graph.entity.SQLiteDbUnit;
import org.droidslicer.graph.entity.SUseUnit;
import org.droidslicer.graph.entity.ServiceUnit;
import org.droidslicer.graph.entity.SharedPreferencesUnit;
import org.droidslicer.graph.entity.SocketUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.graph.entity.UriCommUnit;
import org.droidslicer.graph.entity.UrlConnectionUnit;
import org.droidslicer.graph.entity.VirtualPermissionUnit;
import org.droidslicer.util.Utils;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.UnknownValue;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.StringStuff;

public class PropertiesPane extends JPanel
{
	protected class EntityPropertiesBuilder extends EntityVisitor
	{
		private final EntityTableModel mModel;
		private final BehaviorGraph mGraph;
		public EntityPropertiesBuilder(EntityTableModel model, BehaviorGraph graph)
		{
			if(model == null || graph == null)
				throw new NullPointerException();
			mModel = model;
			mGraph = graph;
		}
		@Override
		public boolean visitComponentUnit(ComponentUnit entity)
		{
			TypeReference typeRef = entity.getType();
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "Entry component"});
			mModel.addRow(new Object[]{PROP_NAME_CLASS, typeRef});
			IClass clazz = mCha.lookupClass(typeRef);
			if(clazz != null)
			{
				IClass superClass = clazz.getSuperclass();
				int ancestors = 1;
				while(superClass != null)
				{
					mModel.addRow(new Object[]{"ancestor-" + ancestors, StringStuff.jvmToBinaryName(superClass.getName().toString())});
					if(superClass.getClassLoader().getReference().equals(ClassLoaderReference.Primordial))
						break;
					superClass = superClass.getSuperclass();
					++ancestors;
				}
				for(IClass inter : clazz.getAllImplementedInterfaces())
				{
					mModel.addRow(new Object[]{"implements", StringStuff.jvmToBinaryName(inter.getName().toString())});
				}
			}
			return true;
		}
		@Override
		public boolean visitIntentCommUnit(IntentCommUnit unit)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "Intent ICC parameters"});
			Class<? extends AppComponentUnit> targetType = unit.getTargetEntityType();
			String targetTypeStr;
			if(targetType.equals(ActivityUnit.class))
			{
				targetTypeStr = "activity";				
			}
			else if(targetType.equals(ProviderUnit.class))
			{
				targetTypeStr = "provider";
			}
			else if(targetType.equals(ReceiverUnit.class))
			{
				targetTypeStr = "receiver";
			}
			else if(targetType.equals(ServiceUnit.class))
			{
				targetTypeStr = "service";
			}
			else
				throw new IllegalArgumentException("Illegal target component type "+ targetType);
			
			mModel.addRow(new Object[]{PROP_NAME_TARGET_CLASS, targetTypeStr});
			mModel.addRow(new Object[]{PROP_NAME_INTENT, unit.getIntentValue()});
			return false;
		}
		@Override
		public boolean visitUriCommUnit(UriCommUnit unit)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "URI ICC parameters"});
			mModel.addRow(new Object[]{PROP_NAME_TARGET_METHOD, unit.getTargetMethod().getName().toString()});
			mModel.addRow(new Object[]{PROP_NAME_URI, unit.getUriValue()});
			return false;
		}
		@Override
		public boolean visitICCParamCallerUnit(ICCParamCallerUnit unit)
		{
			for(RelationEntity relation : mGraph.outgoingEdgesOf(unit))
			{
				UnitEntity dst = mGraph.getEdgeTarget(relation);
				if(!(dst instanceof ComponentUnit))
					continue;
				ComponentUnit comp = (ComponentUnit)dst;
				mModel.addRow(new Object[]{PROP_NAME_POSSIBLE_TARGET, comp.getType()});
			}
			return true;
		}
		@Override
		public boolean visitICCReturnCallerUnit(ICCReturnCallerUnit unit)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "ICC return caller"});
			return true;
		}
		@Override
		public boolean visitICCParamCalleeUnit(ICCParamCalleeUnit unit)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "Entry method parameters"});
			TypeReference typeRef = unit.getType();
			mModel.addRow(new Object[]{PROP_NAME_CLASS, typeRef});
			MethodReference methodRef = unit.getMethod();
			mModel.addRow(new Object[]{PROP_NAME_METHOD, methodRef});
			return true;
		}
		@Override
		public boolean visitICCReturnCalleeUnit(ICCReturnCalleeUnit entity)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "Entry method return values"});
			MethodReference methodRef = entity.getMethod();
			mModel.addRow(new Object[]{PROP_NAME_CLASS, methodRef.getDeclaringClass()});
			mModel.addRow(new Object[]{PROP_NAME_METHOD, methodRef});
			return true;
		}
		public boolean visitInstructionUnit(IInstructionUnit unit)
		{
			CGNode node = unit.getNode();
			SSAInstruction inst = node.getIR().getInstructions()[unit.getInstructionIndex()];
			MethodReference methodRef = node.getMethod().getReference();
			mModel.addRow(new Object[]{PROP_NAME_ORIGIN_CLASS, methodRef.getDeclaringClass()});
			mModel.addRow(new Object[]{PROP_NAME_ORIGIN_METHOD, methodRef});
			mModel.addRow(new Object[]{PROP_NAME_INST, inst});
			return false;
		}
		@Override
		public boolean visitFileInputUnit(FileInputUnit unit)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "File input"});
			mModel.addRow(new Object[]{PROP_NAME_FILE_SRC, unit.getPathValue()});
			return false;
		}
		@Override
		public boolean visitFileOutputUnit(FileOutputUnit unit)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "File output"});
			mModel.addRow(new Object[]{PROP_NAME_FILE_SRC, unit.getPathValue()});
			return false;
		}
		@Override
		public boolean visitInvocationUnit(InvocationUnit unit)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "Invocation"});
			mModel.addRow(new Object[]{PROP_NAME_TARGET_CLASS, unit.getTargetMethod().getDeclaringClass()});
			mModel.addRow(new Object[]{PROP_NAME_TARGET_METHOD, unit.getTargetMethod()});
			for(String perm : unit.getPermissions())
				mModel.addRow(new Object[]{PROP_NAME_PERM, perm});
			int nParam = unit.getNumberOfParameters();
			for(int i = 0; i < nParam; ++i)
			{
				ConcreteValue val = unit.getParamValue(i);
				if(val == null)
					val = UnknownValue.getInstance();
				mModel.addRow(new Object[]{PROP_NAME_PARAM + i, val});
			}
			return false;
		}
		@Override
		public boolean visitSocketUnit(SocketUnit entity)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "Socket"});
			mModel.addRow(new Object[]{PROP_NAME_INFO, entity.getSocketValue()});
			return false;
		}
		@Override
		public boolean visitSharedPreferencesUnit(SharedPreferencesUnit entity)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "SharedPreferences"});
			mModel.addRow(new Object[]{PROP_NAME_INFO, entity.getSharedPreferencesValue()});
			return false;
		}
		@Override
		public boolean visitSQLiteDbUnit(SQLiteDbUnit entity)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "SQLite Database"});
			mModel.addRow(new Object[]{PROP_NAME_INFO, entity.getValue()});
			return false;
		}
		@Override
		public boolean visitUrlConnectionUnit(UrlConnectionUnit entity)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "URL connection"});
			mModel.addRow(new Object[]{PROP_NAME_URL, entity.getUrlValue()});
			return false;
		}
		@Override
		public boolean visitVirtualPermissionUnit(VirtualPermissionUnit unit)
		{
			mModel.addRow(new Object[]{PROP_NAME_TYPE, "Virtual permission-use"});
			return false;
		}
		@Override
		public boolean visitSUseUnit(SUseUnit unit)
		{
			for(String perm : unit.getPermissions())
			{
				mModel.addRow(new Object[]{PROP_NAME_PERM, perm});	
			}
			for(RelationEntity relation : mGraph.incomingEdgesOf(unit))
			{
				if(!(relation instanceof ComponentReachRelation))
					continue;
				UnitEntity src = mGraph.getEdgeSource(relation);
				if(!(src instanceof ComponentUnit))
					continue;
				ComponentUnit comp = (ComponentUnit)src;
				mModel.addRow(new Object[]{PROP_NAME_REACHING_COMP, comp.getType()});
			}
			return true;
		}
		@Override
		public boolean visitICCUnit(ICCUnit unit)
		{
			for(RelationEntity relation : mGraph.incomingEdgesOf(unit))
			{
				if(!(relation instanceof ComponentReachRelation))
					continue;
				UnitEntity src = mGraph.getEdgeSource(relation);
				if(!(src instanceof ComponentUnit))
					continue;
				ComponentUnit comp = (ComponentUnit)src;
				mModel.addRow(new Object[]{PROP_NAME_REACHING_COMP, comp.getType()});
			}
			return false;
		}
		public void build(Entity entity)
		{
			mModel.removeRows();
			entity.visit(this);
			if(entity instanceof IInstructionUnit)
				visitInstructionUnit((IInstructionUnit)entity);
		}
	}
	protected class EntityTableModel extends DefaultTableModel
	{
		private static final long serialVersionUID = 4103856536155080571L;
		private Entity mEntity = null;
		private BehaviorGraph mGraph = null;
		private EntityPropertiesBuilder mPropsBuilder = null;
		public EntityTableModel()
		{
			super(new String[]{"name", "value"}, 0);
		}
		public void removeRows()
		{
			int nRows = getRowCount();
			while(nRows > 0)
			{
				removeRow(nRows - 1);
				--nRows;
			}
		}
		public void setBehaviorGraph(BehaviorGraph graph)
		{
			if(mGraph != graph && graph != null)
			{
				mPropsBuilder = new EntityPropertiesBuilder(this, graph);
			}
			mGraph = graph;
		}
		public void setEntity(Entity entity)
		{
			if(mEntity == entity)
				return;
			mEntity = entity;
			if(entity == null)
				return;
			if(mPropsBuilder == null || mGraph == null)
				throw new IllegalStateException("Behavior graph must have been set");
			mPropsBuilder.build(mEntity);
		}
		@Override
		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
	}
	protected final static String PROP_NAME_TYPE = "type";
	protected final static String PROP_NAME_CLASS = "class";
	protected final static String PROP_NAME_METHOD = "method";
	protected final static String PROP_NAME_FILE_SRC = "file";
	protected final static String PROP_NAME_ORIGIN_CLASS = "origin-class";
	protected final static String PROP_NAME_ORIGIN_METHOD = "origin-method";
	protected final static String PROP_NAME_INST = "instruction";
	protected final static String PROP_NAME_INFO = "info";
	protected final static String PROP_NAME_URL = "URL";
	protected final static String PROP_NAME_INTENT = "intent";
	protected final static String PROP_NAME_URI = "URI";
	protected final static String PROP_NAME_TARGET_CLASS = "target class";
	protected final static String PROP_NAME_TARGET_METHOD = "target method";
	protected final static String PROP_NAME_POSSIBLE_TARGET = "possible target";
	protected final static String PROP_NAME_POSSIBLE_ALIAS = "possible alias";
	protected final static String PROP_NAME_PERM = "permission";
	protected final static String PROP_NAME_PARAM = "param";
	protected final static String PROP_NAME_REACHING_COMP = "reaching component";
	protected static final int COL_IDX_NAME = 0;
	protected static final int COL_IDX_VALUE = 1;
	private IClassHierarchy mCha;
	private static final long serialVersionUID = -1327805050651759225L;
	private final JTable mTable;
	private final EntityTableModel mTableModel;
	private final TableCellRenderer mTypeRefRenderer = new DefaultTableCellRenderer()
	{
		private static final long serialVersionUID = 5987176929956506921L;
		protected void setValue(Object value)
		{
			TypeReference typeRef = (TypeReference)value;
			if(typeRef == null)
				setText("");
			else
			{
				String text = StringStuff.jvmToBinaryName(typeRef.getName().toString());
				setText(text);
				setToolTipText(text);
			}
		}
	};
	private final TableCellRenderer mMethodRefRenderer = new DefaultTableCellRenderer()
	{
		private static final long serialVersionUID = -4776384060548479783L;

		protected void setValue(Object value)
		{
			MethodReference methodRef = (MethodReference)value;
			if(methodRef == null)
				setText("");
			else
			{
				String text = Utils.deploymentMethodString(methodRef);
				setText(text);
				setToolTipText(text);
			}
		}
	};
	
	public PropertiesPane()
	{
		mTableModel = new EntityTableModel();
		mTable = new JTable(mTableModel)
		{
			private static final long serialVersionUID = 7640599402648797285L;
			@Override
			public TableCellRenderer getCellRenderer(int row, int column)
			{
				Object val = getValueAt(row, column);
				if(val instanceof TypeReference)
					return mTypeRefRenderer;
				else if(val instanceof MethodReference)
					return mMethodRefRenderer;
				else
					return super.getCellRenderer(row, column);
			}
		};
		mTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		TableColumnModel colModel = mTable.getColumnModel();
		colModel.getColumn(COL_IDX_NAME).setPreferredWidth(200);
		colModel.getColumn(COL_IDX_VALUE).setPreferredWidth(1500);
		setLayout(new BorderLayout());
		add(new JScrollPane(mTable));
	}
	public void setBehaviorGraph(BehaviorGraph graph)
	{
		mTableModel.setBehaviorGraph(graph);
	}
	public void setClassHierarchy(IClassHierarchy cha)
	{
		mCha = cha;
	}
	public void setEntity(Entity entity)
	{
		EntityTableModel entityModel = (EntityTableModel)mTable.getModel();
		entityModel.setEntity(entity);
	}

}
