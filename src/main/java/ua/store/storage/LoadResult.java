package ua.store.storage;

import java.util.List;

/**
 * Immutable summary of loading entities from a file into a repository.
 *
 * @param totalRead total number of records read from the file
 * @param addedCount number of valid unique records added to the repository
 * @param rejected list of rejected records or file errors
 */
public record LoadResult(int totalRead, int addedCount, List<RejectedRecord> rejected) {

    public LoadResult {
        rejected = (rejected != null) ? List.copyOf(rejected) : List.of();
    }

    public int rejectedCount() {
        return rejected.size();
    }
}
