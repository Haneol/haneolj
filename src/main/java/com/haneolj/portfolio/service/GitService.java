package com.haneolj.portfolio.service;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class GitService {

    @Value("${obsidian.repo.url}")
    private String obsidianRepoUrl;

    @Value("${obsidian.repo.branch}")
    private String obsidianRepoBranch;

    @Value("${obsidian.repo.local-path}")
    private String obsidianLocalPath;

    @Value("${github.username}")
    private String githubUsername;

    @Value("${github.token}")
    private String githubToken;


    // GitHub 자격 증명 제공자 생성
    private CredentialsProvider getCredentialsProvider() {
        if (githubToken != null && !githubToken.isEmpty()) {
            return new UsernamePasswordCredentialsProvider(githubUsername, githubToken);
        }
        return null;
    }

    // 저장소가 로컬에 있는지 확인
    // 1. 존재하지 않는다? git clone 하기
    // 2. 존재한다? git pull 하기
    public String ensureRepository() {
        Path repoPath = Paths.get(obsidianLocalPath);

        try {
            if (isGitRepository(repoPath)) {
                pullRepository(repoPath);
            } else {
                cloneRepository(repoPath);
            }
            return repoPath.toString();
        } catch (Exception e) {
            log.error("저장소 확인 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("저장소를 사용할 수 없습니다", e);
        }
    }

    // repo 경로가 Git 경로인지 확인
    private boolean isGitRepository(Path path) {
        Path gitDir = path.resolve(".git");
        return Files.exists(gitDir) && Files.isDirectory(gitDir);
    }


    // git pull
    private void pullRepository(Path repoPath) throws IOException, GitAPIException {
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(repoPath.resolve(".git").toFile())
                .build();

        try (Git git = new Git(repository)) {
            git.pull()
                    .setRemoteBranchName(obsidianRepoBranch)
                    .setCredentialsProvider(getCredentialsProvider())
                    .call();
        }
    }

    // git clone
    private void cloneRepository(Path repoPath) throws GitAPIException, IOException {
        // 부모 디렉토리가 존재하는지 확인
        Files.createDirectories(repoPath.getParent());

        // 디렉토리가 존재하지만 Git 이 아닌 경우 삭제
        if (Files.exists(repoPath)) {
            deleteDirectory(repoPath.toFile());
        }

        Git.cloneRepository()
                .setURI(obsidianRepoUrl)
                .setDirectory(repoPath.toFile())
                .setBranch(obsidianRepoBranch)
                .setCredentialsProvider(getCredentialsProvider())
                .call();
    }

    // 디렉토리 재귀적으로 삭제
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    // 파일 생성 시간 가져오기
    public LocalDateTime getFileCreationDate(Path filePath) {
        try {
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(Paths.get(obsidianLocalPath, ".git").toFile())
                    .build();

            try (Git git = new Git(repository)) {

                // 저장소 루트로부터의 상대 경로 가져오기
                String relativePath = Paths.get(obsidianLocalPath).relativize(filePath).toString();

                // 이 파일에 대한 모든 커밋을 커밋 시간 순으로 정렬하여 가져오기
                Iterable<RevCommit> commits = git.log()
                        .addPath(relativePath.replace('\\', '/'))
                        .call();

                // 가장 오래된 커밋 찾기
                RevCommit oldestCommit = null;
                for (RevCommit commit : commits) {
                    oldestCommit = commit;
                }

                if (oldestCommit != null) {
                    // 커밋 시간을 LocalDateTime으로 변환
                    return LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(oldestCommit.getCommitTime()),
                            ZoneId.systemDefault());
                }
            }

            // Git 이력이 없는 경우 파일 생성 시간으로 대체
            return LocalDateTime.ofInstant(
                    (Files.getAttribute(filePath, "creationTime") != null)
                            ? ((FileTime)Files.getAttribute(filePath, "creationTime")).toInstant()
                            : Files.getLastModifiedTime(filePath).toInstant(),
                    ZoneId.systemDefault());
        } catch (Exception e) {
            log.warn("파일 생성 시간을 가져올 수 없습니다: {}", e.getMessage());
            // 대안으로 마지막 수정 시간 반환
            try {
                return LocalDateTime.ofInstant(
                        Files.getLastModifiedTime(filePath).toInstant(),
                        ZoneId.systemDefault());
            } catch (IOException ex) {
                log.error("파일 시간 정보를 가져올 수 없습니다: {}", ex.getMessage());
                return LocalDateTime.now(); // 그것도 안되면 현재 시간 뿌리기
            }
        }
    }
}