% HASHMAP2STRUCT Converts a Java HashMap to a Matlab structure
function s = hashmap2struct(jh)
         k = jh.keySet().toArray();
         v = jh.values().toArray();
         for i=1:jh.size        
             s.(k(i)) = v(i);
         end
end
