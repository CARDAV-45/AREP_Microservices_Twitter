package arep.edu.co.microservices.service;

import arep.edu.co.microservices.dto.PostRequest;
import arep.edu.co.microservices.dto.PostResponse;
import arep.edu.co.microservices.model.Post;
import arep.edu.co.microservices.model.User;
import arep.edu.co.microservices.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;


    @Transactional(readOnly = true)
    public List<PostResponse> getStream() {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PostResponse::from)
                .toList();
    }

    @Transactional
    public PostResponse createPost(PostRequest request, User author) {
        Post post = Post.builder()
                .content(request.content())
                .author(author)
                .build();
        return PostResponse.from(postRepository.save(post));
    }
}
