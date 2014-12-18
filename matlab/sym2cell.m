function f=sym2cell(g)
n = numel(g(:));
f = cell(1, n);
for index = 1:n
f{index} = char(g(index));
end
end