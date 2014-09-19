package operator.snpeff;

import java.util.HashMap;

public class VarEffects {
	public final static HashMap<String, Integer> effects = new HashMap<String, Integer>();
	static { 
		// variant effects from snpEff 3.6 and 4.0
		// 3.6 varinats are in all caps
		// the int score shows which takes precidence in pipeline annotation
		// the trailing comment for each shows associated snpEff predicted impact
		effects.put("chromosome", 3); //HIGH
		effects.put("CHROMOSOME_LARGE_DELETION", 3); //HIGH
		effects.put("coding_sequence_variant", 2); //MODERATE
		effects.put("CODON_CHANGE", 2); //MODERATE
		effects.put("CDS", 2); //MODERATE
		effects.put("inframe_insertion", 2); //MODERATE
		effects.put("CODON_INSERTION", 2); //MODERATE
		effects.put("disruptive_inframe_insertion", 2); //MODERATE
		effects.put("CODON_CHANGE_PLUS_CODON_INSERTION", 2); //MODERATE
		effects.put("inframe_deletion", 2); //MODERATE
		effects.put("CODON_DELETION", 2); //MODERATE
		effects.put("disruptive_inframe_deletion", 2); //MODERATE
		effects.put("CODON_CHANGE_PLUS_CODON_DELETION", 2); //MODERATE
		effects.put("downstream_gene_variant", 1); //MODIFIER
		effects.put("DOWNSTREAM", 1); //MODIFIER
		effects.put("exon_variant", 1); //MODIFIER
		effects.put("EXON", 1); //MODIFIER
		effects.put("exon_loss_variant", 3); //HIGH
		effects.put("EXON_DELETED", 3); //HIGH
		effects.put("frameshift_variant", 3); //HIGH
		effects.put("FRAME_SHIFT", 3); //HIGH
		effects.put("gene_variant", 1); //MODIFIER
		effects.put("GENE", 1); //MODIFIER
		effects.put("intergenic_region", 1); //MODIFIER
		effects.put("INTERGENIC", 1); //MODIFIER
		effects.put("conserved_intergenic_variant", 1); //MODIFIER
		effects.put("INTERGENIC_CONSERVED", 1); //MODIFIER
		effects.put("intragenic_variant", 1); //MODIFIER
		effects.put("INTRAGENIC", 1); //MODIFIER
		effects.put("intron_variant", 1); //MODIFIER
		effects.put("INTRON", 1); //MODIFIER
		effects.put("conserved_intron_variant", 1); //MODIFIER
		effects.put("INTRON_CONSERVED", 1); //MODIFIER
		effects.put("miRNA", 1); //MODIFIER
		effects.put("MICRO_RNA", 1); //MODIFIER
		effects.put("missense_variant", 2); //MODERATE
		effects.put("NON_SYNONYMOUS_CODING", 2); //MODERATE
		effects.put("initiator_codon_variant", 2); //LOW
		effects.put("NON_SYNONYMOUS_START", 3); //LOW
		effects.put("stop_retained_variant", 2); //LOW
		effects.put("NON_SYNONYMOUS_STOP", 2); //LOW
		effects.put("rare_amino_acid_variant", 3); //HIGH
		effects.put("RARE_AMINO_ACID", 3); //HIGH
		effects.put("splice_acceptor_variant", 3); //HIGH
		effects.put("SPLICE_SITE_ACCEPTOR", 3); //HIGH
		effects.put("splice_donor_variant", 3); //HIGH
		effects.put("SPLICE_SITE_DONOR", 3); //HIGH
		effects.put("splice_region_variant", 2); //LOW
		effects.put("SPLICE_SITE_REGION", 2); //LOW
		effects.put("SPLICE_SITE_BRANCH", 2); //LOW
		effects.put("SPLICE_SITE_BRANCH_U22", 3); //MODERATE
		effects.put("stop_lost", 3); //HIGH
		effects.put("STOP_LOST", 3); //HIGH
		effects.put("5_prime_UTR_premature_start_codon_gain_variant", 2); //LOW
		effects.put("START_GAINED", 2); //LOW
		effects.put("start_lost", 4); //HIGH
		effects.put("START_LOST", 4); //HIGH
		effects.put("stop_gained", 4); //HIGH
		effects.put("STOP_GAINED", 4); //HIGH;
		effects.put("synonymous_variant", 2); //LOW
		effects.put("SYNONYMOUS_CODING", 2); //LOW
		effects.put("start_retained", 2); //LOW
		effects.put("SYNONYMOUS_START", 2); //LOW
		effects.put("SYNONYMOUS_STOP", 2); //LOW
		effects.put("transcript_variant", 1); //MODIFIER
		effects.put("TRANSCRIPT", 1); //MODIFIER
		effects.put("regulatory_region_variant", 1); //MODIFIER
		effects.put("REGULATION", 1); //MODIFIER
		effects.put("upstream_gene_variant", 1); //MODIFIER
		effects.put("UPSTREAM", 1); //MODIFIER
		effects.put("3_prime_UTR_variant", 1); //MODIFIER
		effects.put("UTR_3_PRIME", 1); //MODIFIER
		effects.put("3_prime_UTR_truncation_+_exon_loss", 2); //MODERATE
		effects.put("UTR_3_DELETED", 2); //MODERATE
		effects.put("5_prime_UTR_variant", 1); //MODIFIER
		effects.put("UTR_5_PRIME", 1); //MODIFIER
		effects.put("5_prime_UTR_truncation_+_exon_loss_variant", 2); //MODERATE
		effects.put("UTR_5_DELETED", 2); //MODERATE
	}
}
