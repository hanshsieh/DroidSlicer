package org.droidslicer.graph.entity;

public interface IEntityVisitor
{
	public boolean visitEntity(Entity entity);
	public boolean visitRelation(RelationEntity relation);
	public boolean visitUnit(UnitEntity unit);
	public boolean visitCall2ReturnRelation(Call2ReturnRelation relation);
	public boolean visitComponentReachRelation(ComponentReachRelation relation);
	public boolean visitDataDependencyRelation(DataDependencyRelation relation);
	public boolean visitFileSystemDataRelation(FileSystemDataRelation relation);
	public boolean visitICCRelation(ICCRelation relation);
	public boolean visitIntentCommRelation(IntentCommRelation relation);
	public boolean visitUriCommRelation(UriCommRelation relation);
	public boolean visitComponentUnit(ComponentUnit unit);
	public boolean visitAppComponentUnit(AppComponentUnit unit);
	public boolean visitActivityUnit(ActivityUnit unit);
	public boolean visitProviderUnit(ProviderUnit unit);
	public boolean visitReceiverUnit(ReceiverUnit unit);
	public boolean visitServiceUnit(ServiceUnit unit);
	public boolean visitApplicationUnit(ApplicationUnit unit);
	public boolean visitICCUnit(ICCUnit unit);
	public boolean visitICCParamCalleeUnit(ICCParamCalleeUnit unit);
	public boolean visitICCParamCallerUnit(ICCParamCallerUnit unit);
	public boolean visitICCReturnCalleeUnit(ICCReturnCalleeUnit unit);
	public boolean visitICCReturnCallerUnit(ICCReturnCallerUnit unit);
	public boolean visitIntentCommUnit(IntentCommUnit unit);
	public boolean visitUriCommUnit(UriCommUnit unit);
	public boolean visitSUseUnit(SUseUnit unit);
	public boolean visitFileUnit(FileUnit unit);
	public boolean visitFileInputUnit(FileInputUnit unit);
	public boolean visitFileOutputUnit(FileOutputUnit unit);
	public boolean visitInvocationUnit(InvocationUnit unit);
	public boolean visitSocketUnit(SocketUnit unit);
	public boolean visitSocketInputUnit(SocketInputUnit unit);
	public boolean visitSocketOutputUnit(SocketOutputUnit unit);
	public boolean visitSQLiteDbUnit(SQLiteDbUnit unit);
	public boolean visitSharedPreferencesUnit(SharedPreferencesUnit unit);
	public boolean visitUrlConnectionUnit(UrlConnectionUnit unit);
	public boolean visitUrlConnectionInputUnit(UrlConnectionInputUnit unit);
	public boolean visitUrlConnectionOutputUnit(UrlConnectionOutputUnit unit);
	public boolean visitVirtualPermissionUnit(VirtualPermissionUnit unit);
}

