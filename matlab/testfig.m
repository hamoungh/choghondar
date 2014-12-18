%h = plot('v6',1:100, sin(1:100), 1:100, sin(1:100))
% 
%  set(gcf, 'PaperUnits', 'inches');
%  set(gcf, 'PaperSize', [8.5 14]);
% % papersize = get(gcf, 'PaperSize')
%  width = 5.5;         % Initialize a variable for width.
%  height = 3;          % Initialize a variable for height.
% % left = (papersize(1)- width)/2
% % bottom = (papersize(2)- height)/2
%  myfiguresize = [0, 0, width, height];
%  set(gcf, 'PaperPosition', myfiguresize);

% he default string for an object is the value of the object's DisplayName  
% property, if you have defined a value for DisplayName 
%     (which you can do using the Property Editor or calling set). 
%     Otherwise, legend constructs a string of the form data1, data2, etc. 
%     Setting display names is useful when you are experimenting with legends and might forget how objects in 
%     a lineseries, for example, are ordered.

%print -depsc -tiff 'figures\myfigure'
function annotation_property_line
dat = rand(50,1);
hLine = plot(dat);
plotMean % Nested function draws a line at mean value
set(get(get(hLine,'Annotation'),'LegendInformation'),...
    'IconDisplayStyle','off'); % Exclude line from legend
legend('mean')
    function plotMean
    xlimits = get(gca,'XLim');
    meanValue = mean(dat);
    meanLine = line([xlimits(1) xlimits(2)],...
		 [meanValue meanValue],'Color','k','LineStyle','-.');
    end
end