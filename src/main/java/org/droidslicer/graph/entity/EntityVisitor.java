package org.droidslicer.graph.entity;

public class EntityVisitor implements IEntityVisitor
{

	@Override
	public boolean visitEntity(Entity entity) {
		return false;
	}

	@Override
	public boolean visitRelation(RelationEntity relation) {
		return false;
	}

	@Override
	public boolean visitUnit(UnitEntity unit) {
		return false;
	}

	@Override
	public boolean visitCall2ReturnRelation(Call2ReturnRelation relation) {
		return false;
	}

	@Override
	public boolean visitComponentReachRelation(ComponentReachRelation relation) {
		return false;
	}

	@Override
	public boolean visitDataDependencyRelation(DataDependencyRelation relation) {
		return false;
	}

	@Override
	public boolean visitFileSystemDataRelation(FileSystemDataRelation relation) {
		return false;
	}

	@Override
	public boolean visitICCRelation(ICCRelation relation) {
		return false;
	}

	@Override
	public boolean visitIntentCommRelation(IntentCommRelation relation) {
		return false;
	}

	@Override
	public boolean visitUriCommRelation(UriCommRelation relation) {
		return false;
	}

	@Override
	public boolean visitComponentUnit(ComponentUnit unit) {
		return false;
	}

	@Override
	public boolean visitAppComponentUnit(AppComponentUnit unit) {
		return false;
	}

	@Override
	public boolean visitActivityUnit(ActivityUnit unit) {
		return false;
	}

	@Override
	public boolean visitProviderUnit(ProviderUnit unit) {
		return false;
	}

	@Override
	public boolean visitReceiverUnit(ReceiverUnit unit) {
		return false;
	}

	@Override
	public boolean visitServiceUnit(ServiceUnit unit) {
		return false;
	}

	@Override
	public boolean visitApplicationUnit(ApplicationUnit unit) {
		return false;
	}

	@Override
	public boolean visitICCUnit(ICCUnit unit) {
		return false;
	}

	@Override
	public boolean visitICCParamCalleeUnit(ICCParamCalleeUnit unit) {
		return false;
	}

	@Override
	public boolean visitICCParamCallerUnit(ICCParamCallerUnit unit) {
		return false;
	}

	@Override
	public boolean visitICCReturnCalleeUnit(ICCReturnCalleeUnit unit) {
		return false;
	}

	@Override
	public boolean visitICCReturnCallerUnit(ICCReturnCallerUnit unit) {
		return false;
	}

	@Override
	public boolean visitSUseUnit(SUseUnit unit) {
		return false;
	}

	@Override
	public boolean visitFileUnit(FileUnit unit) {
		return false;
	}

	@Override
	public boolean visitInvocationUnit(InvocationUnit unit) {
		return false;
	}

	@Override
	public boolean visitSocketUnit(SocketUnit unit) {
		return false;
	}

	@Override
	public boolean visitSQLiteDbUnit(SQLiteDbUnit unit) {
		return false;
	}

	@Override
	public boolean visitUrlConnectionUnit(UrlConnectionUnit unit) {
		return false;
	}

	@Override
	public boolean visitVirtualPermissionUnit(VirtualPermissionUnit unit) {
		return false;
	}

	@Override
	public boolean visitSharedPreferencesUnit(SharedPreferencesUnit unit)
	{
		return false;
	}

	@Override
	public boolean visitIntentCommUnit(IntentCommUnit unit)
	{
		return false;
	}

	@Override
	public boolean visitUriCommUnit(UriCommUnit unit)
	{
		return false;
	}

	@Override
	public boolean visitFileInputUnit(FileInputUnit unit) 
	{
		return false;
	}

	@Override
	public boolean visitFileOutputUnit(FileOutputUnit unit)
	{
		return false;
	}

	@Override
	public boolean visitUrlConnectionInputUnit(UrlConnectionInputUnit unit) 
	{
		return false;
	}

	@Override
	public boolean visitUrlConnectionOutputUnit(UrlConnectionOutputUnit unit) 
	{
		return false;
	}

	@Override
	public boolean visitSocketInputUnit(SocketInputUnit unit) 
	{
		return false;
	}

	@Override
	public boolean visitSocketOutputUnit(SocketOutputUnit unit)
	{
		// TODO Auto-generated method stub
		return false;
	}

}
