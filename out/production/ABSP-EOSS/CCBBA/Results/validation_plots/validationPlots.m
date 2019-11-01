function [] = validationPlots()
    clc; close all;
    addpath("./figure_3");
    addpath("./figure_5");
    
    % Figure 3
    results3 = readData("figure_3", 55); 
    figureThree(results3);
    
    % Figure 5
    results5 = readData("figure_5", 1); 
    figureFive(results5);
end