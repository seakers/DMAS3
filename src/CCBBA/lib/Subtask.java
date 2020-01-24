package CCBBA.lib;

import java.util.ArrayList;

public class Subtask {
    private String main_task;
    private ArrayList<String> dep_tasks;
    private ArrayList<Integer> dep_nums;
    private int main_num;
    private String name;
    private Task parentTask;

    public Subtask(String new_main, int new_num, Task parent){
        main_task = new_main;
        main_num = new_num;
        dep_tasks = new ArrayList<String>();
        dep_nums = new ArrayList<Integer>();
        name = new_main + "_{}";
        parentTask = parent;
    }

    public void addDep_task(String new_task, int new_num){
        dep_tasks.add(new_task);
        dep_nums.add(new_num);
        String temp_name = "";
        name = main_task;
        for (int i = 0; i < dep_tasks.size(); i++) {
            if (i == 0) {
                temp_name = temp_name + "_{";
            }
            temp_name = temp_name + dep_tasks.get(i);
            if (i != (dep_tasks.size() - 1)) {
                temp_name = temp_name + ", ";
            } else {
                temp_name = temp_name + "}";
            }
            name = name + temp_name;
        }
    }

    public ArrayList<Integer> getDep_nums(){ return dep_nums; }
    public int getMain_num(){ return main_num; }
}
