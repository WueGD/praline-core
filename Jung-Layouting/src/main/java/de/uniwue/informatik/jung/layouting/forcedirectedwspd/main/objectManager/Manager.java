package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.Dialog;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;

import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.Scanner;

/**
 * Object-manager
 * <br>
 * An instance of this class can help the user to select objects controlled/managed/linked by this "manager". 
 */
public abstract class Manager<E> {
	
	/**
	 * Object itself + its name
	 */
	private LinkedList<Tuple<E, String>> allObjects = new LinkedList<Tuple<E, String>>();
	
	private boolean listHasBeenInitialzied;
	
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public Tuple<E, String> getObjectWithObjectname(int index){
		initIfNotDoneYet();
		return new Tuple<E, String>(allObjects.get(index).get1(), allObjects.get(index).get2());
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public Tuple<E, String> getObjectWithObjectnameWithoutInitializing(int index){
		return new Tuple<E, String>(allObjects.get(index).get1(), allObjects.get(index).get2());
	}
	
	/**
	 * Removes a object (incl. objectname) from the list of the objects registered in this manager.
	 */
	public void removeObject(Tuple<E,String> object){
		initIfNotDoneYet();
		int index = allObjects.indexOf(object);
		allObjects.remove(index);
	}
	
	/**
	 * Removes a object (incl. objectname) from the list of the objects registered in this manager.
	 */
	public void removeObject(E object){
		initIfNotDoneYet();
		while(true){
			try{
				for(Tuple<E, String> t: allObjects){
					if(t.get1()==object){
						allObjects.remove(t);
					}
				}
				break;
			}
			catch(ConcurrentModificationException e){ }
		}
	}
	
	/**
	 * Removes a object (incl. objectname) from the list of the objects registered in this manager
	 */
	public void removeObject(int index){
		initIfNotDoneYet();
		allObjects.remove(index);
	}
	
	
	public int getObjectCountWithoutInitializing(){
		return allObjects.size();
	}
	
	public int getObjectCount(){
		initIfNotDoneYet();
		return allObjects.size();
	}
	
	/**
	 * This must be implemented in the concrete class.
	 * In this method the already initialized {@link LinkedList} {@link Manager#allObjects}
	 * must be filled with {@link Tuple}s of the objects themselve + names to them.
	 * For that you may call {@link Manager#addToObjectList(Object, String)}
	 */
	protected abstract void initialize();
	
	/**
	 * May be called in {@link Manager#initialize()} to fill the list.
	 * 
	 * @param object
	 * @param objectName
	 */
	protected void addToObjectList(E object, String objectName){
		allObjects.add(new Tuple<E,String>(object, objectName));
	}
	
	protected void initIfNotDoneYet(){
		if(!listHasBeenInitialzied){
			initialize();
			listHasBeenInitialzied = true;
		}
	}
	
	
	public E assistantForSelectingOneObject(Scanner sc){
		initIfNotDoneYet();
		String question = "Please select one index:";
		for(int i=0; i<allObjects.size(); i++){
			question += "\n"+i+": "+allObjects.get(i).get2();
		}
		int selection = Dialog.getNextInt(sc, question, 0, allObjects.size()-1);
		return allObjects.get(selection).get1();
	}
	
	public LinkedList<E> assistantForSelectingMultipleObjects(Scanner sc){
		initIfNotDoneYet();
		String question = "Please select indices (separated by ','):";
		for(int i=1; i<=allObjects.size(); i++){
			question += "\n"+i+": "+allObjects.get(i-1).get2(); //-1 because i starts with 1 and not with 0
		}
		question += "\n0: select all";
		String input = Dialog.getNextLine(sc, question);
		LinkedList<E> selectionList = new LinkedList<E>();
		//case 1: 0 was entered
		try{
			int extracted = Integer.parseInt(input);
			if(extracted==0){
				for(Tuple<E,String> t: allObjects){
					selectionList.add(t.get1());
				}
				return selectionList;
			}
		}
		catch(NumberFormatException e){ }
		
		//Fall 2: sth else was entered
		boolean validInput = false;
		while(!validInput){
			validInput = true;
			selectionList.clear();
			int i = 0; //Index determinig in which char in the line is the currently treated one
			int intInput = 0; //number having been inserted before i
			while(input.length()==0){
				if(Dialog.assistantConfirm(sc, "Nothing selected. Continue with the empty selection?")){
					break;
				}
				else{
					input = Dialog.getNextLine(sc, question);
				}
			}
			while(i<input.length()){
				int extracted = -1;
				try{
					extracted = Integer.parseInt(input.charAt(i)+"");
					intInput = intInput*10+extracted;
				}
				catch(NumberFormatException e){
					if(input.charAt(i)==',' || input.charAt(i)==';'){
						if(intInput < 1 || intInput > allObjects.size()){
							validInput = false;
							System.out.println("Invalid input. Enter only numbers in the listed range separated by a comma (',').");
							input = sc.nextLine();
							break;
						}
						selectionList.add(allObjects.get(intInput-1).get1()); //-1 because index started with 1 and not with 0
						intInput = 0;
					}
					else if(input.charAt(i)!=' ' || input.charAt(i)!='\n' || input.charAt(i)!='\r' || input.charAt(i)!='\t'){
						validInput = false;
						System.out.println("Invalid input. Enter only numbers in the listed range separated by a comma (',').");
						input = sc.nextLine();
						break;
					}
				}
				i++;
			}
			if(intInput==0){
				if(input.charAt(0)=='0'){
					//0 was enterd -> select all
					for(Tuple<E,String> t: allObjects){
						selectionList.add(t.get1());
					}
					return selectionList;
				}
				else{
					validInput = false;
					System.out.println("Invalid input. Enter only numbers in the listed range separated by a comma (',').");
					input = sc.nextLine();
					continue;					
				}
			}
			else{
				if(intInput < 1 || intInput > allObjects.size()){
					validInput = false;
					System.out.println("Invalid input. Enter only numbers in the listed range separated by a comma (',').");
					input = sc.nextLine();
					continue;
				}
				selectionList.add(allObjects.get(intInput-1).get1()); //-1 because index started with 1 and not with 0
			}
		}
		return selectionList;
	}
	
	public LinkedList<E> assistantForExcludingMultipleObjects(Scanner sc){
		System.out.println("A selection of objects can be taken out from the available objects. "
				+ "Make your selection of objects being excluded now.");
		LinkedList<E> toBeExcluded = this.assistantForSelectingMultipleObjects(sc);
		
		LinkedList<E> returnList = new LinkedList<E>();
		for(Tuple<E,String> t: allObjects){
			returnList.add(t.get1());
		}
		for(E e: toBeExcluded){
			returnList.remove(e);
		}
		return returnList;
	}
}
