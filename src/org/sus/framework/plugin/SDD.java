package org.sus.framework.plugin;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.sus.framework.analysis.AnalysisLoader;

@SuppressWarnings({"rawtypes","unchecked"})
public class SDD extends ViewPart {
	private TreeViewer viewer;
	private TableViewer viewer1;
	private TreeParent invisibleRoot;
	public int no_lines =0 ;

	public String[] readFile(String fileName) throws IOException {
	    
		BufferedReader br = new BufferedReader(new FileReader(fileName));
	    try {
	    	//String[] data = {"null"}; 
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();
	        while (line != null) {
	        	sb.append(line);
	            sb.append("\n");
	            no_lines++;
	            line = br.readLine();
	        }
	        System.out.println("no of lines read="+no_lines);
	        //System.out.println(sb.toString());
	        return sb.toString().split("\n");
	        
	    } finally {
	        br.close();
	    }
	}
	        
	class TreeObject implements IAdaptable {
	
		private String name;
		private TreeParent parent;
		private IType resouce;
			
		public TreeObject(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public void setParent(TreeParent parent) {
			this.parent = parent;
		}
			
		public TreeParent getParent() {
			return parent;
		}
			
		public String toString() {
			return getName();
		}
			
		public Object getAdapter(Class key) {
			return null;
		}
		protected IType getResouce() {
			return resouce;
		}
		
		protected void setResouce(IType resouce) {
			this.resouce = resouce;
		}
	}
	class TreeParent extends TreeObject {
		private ArrayList children;
		public TreeParent(String name) {
			super(name);
			children = new ArrayList();
		}
		
		public void addChild(TreeObject child) {
			children.add(child);
			child.setParent(this);
		}
		
		public void removeChild(TreeObject child) {
			children.remove(child);
			child.setParent(null);
		}

		public TreeObject[] getChildren() {
			return (TreeObject[]) children.toArray(new TreeObject[children.size()]);
		}

		public boolean hasChildren() {
			return children.size() > 0;
		}

	}
	class ViewContentProvider implements ITreeContentProvider {
		
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				if (invisibleRoot == null)
					initialize();

				return getChildren(invisibleRoot);
			}
			return getChildren(parent);
		}

		public Object getParent(Object child) {
			if (child instanceof TreeObject) {
				return ((TreeObject) child).getParent();
			}

			return null;
		}

		public Object[] getChildren(Object parent) {

			if (parent instanceof TreeParent) {
				return ((TreeParent) parent).getChildren();
			}

			return new Object[0];
		}

		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeParent)
				return ((TreeParent) parent).hasChildren();
			return false;
		}

	}

	class ViewLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return obj.toString();
		}
		public Image getImage(Object obj) {
			String imageKey = ISharedImages.IMG_OBJ_ELEMENT;

			if (obj instanceof TreeParent)
				imageKey = ISharedImages.IMG_OBJ_FOLDER;
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}

	}

	public void initialize() {
		TreeParent root = new TreeParent("Choose Class");
		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IProject[] projects = workspace.getRoot().getProjects();

			for (int i = 0; i < projects.length; i++) {
				 IJavaProject javaProject = JavaCore.create(projects[i]);
				 IPackageFragment[] packages = javaProject.getPackageFragments();
				 
				  for (int j = 0; j < packages.length; j++) {
					System.out.println(packages[j].getElementName());
					ICompilationUnit[] compilationUnit = packages[j].getCompilationUnits();
					
							for (int k = 0; k < compilationUnit.length; k++) {
								System.out.println(compilationUnit[k].getElementName());
								if(compilationUnit[k].getElementName().endsWith(".java")){
									IType[] classes =  compilationUnit[k].getTypes();
									for(int z=0; z< classes.length; z++){
										String packagePrefix = 
												(packages[j].getElementName() == null || packages[j].getElementName().isEmpty()) ? ""
														: packages[j].getElementName()+".";
										TreeObject obj = new TreeObject(packagePrefix+classes[z].getElementName());
										obj.setResouce(classes[z]);
										root.addChild(obj);
									}
								}
							}
						}
				}
			
		}catch (Exception e) {
			// log exception
		}
		invisibleRoot = new TreeParent("");
		invisibleRoot.addChild(root);
	}


	public SDD() {
	}

	public void createPartControl(Composite parent) {		
		viewer1 = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
			        | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		//Creating Tree Viewer
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(getViewSite());
		hookContextMenu();
		hookDoubleCLickAction();
	}

	

	private void hookDoubleCLickAction() {
		
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = event.getSelection();
				Object selectedElement = ((IStructuredSelection) selection).getFirstElement();
				if (!(selectedElement instanceof TreeObject)) {
					return;
				}else {
					String file_path = "C:\\Users\\Karthik\\Desktop\\sus.txt";
					try{
						AnalysisLoader susLoader = new AnalysisLoader();
						susLoader.main(selectedElement.toString());
						Table table = viewer1.getTable();
						table.setHeaderVisible(true);
						table.setLinesVisible(true);
				
						TableColumn column1 = new TableColumn(table, SWT.LEFT);
						TableColumn column2 = new TableColumn(table, SWT.LEFT);
					
						column1.setWidth(100);
						column1.setText("File");
						column2.setWidth(100);
						column2.setText("Line Number");
					
						String[] data = readFile(file_path);
					
						TableItem[] row = new TableItem[no_lines/2];
						for(int i=0;i<(no_lines/2);i++){
							row[i] = new TableItem(table, SWT.NONE);
						
							System.out.println(data[(2*i)]+data[(2*i)+1]);
							row[i].setText(new String[] {data[2*i],data[2*i+1]});
							
						}
						
					}
					catch(IOException e){
					}
				}
			};
		});
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		Action refresh =new Action() {
			public void run() {
				initialize();
				viewer.refresh();
			}
		};
		refresh.setText("Refresh");	
		menuMgr.add(refresh);
	}
     
	public void setFocus() {
		viewer.getControl().setFocus();	
	}
}