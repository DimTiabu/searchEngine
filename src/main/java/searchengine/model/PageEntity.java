package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "page")
@Data
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(name = "path", nullable = false,
            columnDefinition = "TEXT, INDEX idx_page_path USING BTREE (path(50))")

    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @ToString.Exclude
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
}