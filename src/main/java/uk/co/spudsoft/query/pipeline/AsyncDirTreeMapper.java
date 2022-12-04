/*
 * Copyright (C) 2022 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.pipeline;

import io.vertx.core.Future;
import io.vertx.core.impl.future.CompositeFutureImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.co.spudsoft.dircache.AbstractTree;
import uk.co.spudsoft.dircache.DirCacheTree;

/**
 *
 * @author jtalbut
 */
public class AsyncDirTreeMapper {
  
    /**
     * Map this Directory and all its children (recursively) into a different subclass of {@link AbstractTree}.
     * 
     * Either of the mapping methods may return null, which will not be included in the output structure.
     * This is the recommended approach if empty Directories are to be trimmed from the output.
     * 
     * @param <N> The subtype of AbstractTree.AbstractNode used for generic nodes in the mapped tree.
     * @param <D> The subtype of AbstractTree.AbstractNode used for internal nodes (directories) in the mapped tree.
     * @param <F> The subtype of AbstractTree.AbstractNode used for leaf nodes (files) in the mapped tree.
     * @param dir The directory whose contents are to be mapped.
     * @param dirValidator Method that should return a Future&lt;Boolean> that must be completed with a TRUE value for the directory to be processed.
     * @param dirMapper Method for mapping a Directory and it's already mapped children to a mapped Directory.
     * @param fileMapper Method for mapping a File to a mapped File.
     * @return The result of called dirMapper on this Directory with all of its children mapped.
     */
    public static <N extends AbstractTree.AbstractNode<N>, D extends N, F extends N> Future<D> map(
            DirCacheTree.Directory dir
            , Function<DirCacheTree.Directory, Future<Boolean>> dirValidator
            , BiFunction<DirCacheTree.Directory, List<N>, Future<D>> dirMapper
            , Function<DirCacheTree.File, Future<F>> fileMapper
    ) {
      
      List<Future<? extends N>> futures = dir.getChildren().stream().map(n -> {
                if (n instanceof DirCacheTree.File) {
                  DirCacheTree.File f = (DirCacheTree.File) n;
                  return fileMapper.apply(f);
                } else {
                  DirCacheTree.Directory d = (DirCacheTree.Directory) n;
                  return dirValidator.apply(d)
                          .compose(permitted -> {
                            if (permitted) {
                              return map(d, dirValidator, dirMapper, fileMapper);
                            } else {
                              return Future.succeededFuture();
                            }
                          });
                }
              })
              .collect(Collectors.toList());
      
      return CompositeFutureImpl.all(futures.toArray(size -> new Future[size]))
              .compose(cf -> {
                List<N> children = new ArrayList<>(futures.size());
                for (Future<? extends N> future : futures) {
                  N node = future.result();
                  if (node != null) {
                    children.add(node);
                  }
                }
                return dirMapper.apply(dir, children);
              });
    }
  
}
